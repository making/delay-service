package am.ik.routeservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

public class DelayHandler {
	private static final Logger log = LoggerFactory.getLogger(DelayHandler.class);
	private static final String FORWARDED_URL = "X-CF-Forwarded-Url";
	private static final String PROXY_METADATA = "X-CF-Proxy-Metadata";
	private static final String PROXY_SIGNATURE = "X-CF-Proxy-Signature";
	private final WebClient webClient = WebClient.create();
	private final AtomicReference<Conf> conf = new AtomicReference<>(
			new Conf("random", 10_000));

	public RouterFunction<ServerResponse> route() {
		return RouterFunctions.route(incoming(), this::forward) //
				.andRoute(GET("/"), this::getConf) //
				.andRoute(POST("/"), this::setConf);
	}

	private RequestPredicate incoming() {
		return req -> {
			final HttpHeaders h = req.headers().asHttpHeaders();
			return h.containsKey(FORWARDED_URL) && h.containsKey(PROXY_METADATA)
					&& h.containsKey(PROXY_SIGNATURE);
		};
	}

	Mono<ServerResponse> forward(ServerRequest req) {
		final HttpHeaders headers = headers(req.headers().asHttpHeaders());
		final URI uri = headers.remove(FORWARDED_URL).stream().findFirst()
				.map(URI::create).orElseThrow(() -> new IllegalStateException(
						String.format("No %s header present", FORWARDED_URL)));
		final WebClient.RequestHeadersSpec<?> spec = webClient.method(req.method()) //
				.uri(uri) //
				.headers(h -> h.putAll(headers));
		if (log.isInfoEnabled()) {
			log.info("Incoming Request: <{} {},{}>", req.method(), req.uri(),
					req.headers().asHttpHeaders());
		}
		return req
				.bodyToMono(String.class).<WebClient.RequestHeadersSpec<?>>map(
						((WebClient.RequestBodySpec) spec)::syncBody)
				.switchIfEmpty(Mono.just(spec)) //
				.delayElement(delay()) //
				.flatMap(s -> s.exchange()
						.flatMap(r -> status(r.statusCode())
								.headers(h -> h.putAll(r.headers().asHttpHeaders()))
								.body(r.bodyToMono(String.class), String.class)))
				.doOnTerminate(() -> {
					if (log.isInfoEnabled()) {
						log.info("Outgoing Request: <{} {},{}>", req.method(), uri,
								headers);
					}
				});
	}

	Mono<ServerResponse> setConf(ServerRequest req) {
		return req.bodyToMono(Conf.class) //
				.filter(c -> "random".equals(c.getType()) || "fixed".equals(c.getType()))
				.doOnNext(this.conf::set) //
				.flatMap(c -> ok().syncBody(c)) //
				.switchIfEmpty(badRequest().syncBody(
						singletonMap("error", "'type' must be 'random' or 'fixed'")));

	}

	Mono<ServerResponse> getConf(ServerRequest req) {
		return ok().syncBody(conf.get());
	}

	private Duration delay() {
		Conf conf = this.conf.get();
		int delay;
		if ("random".equals(conf.type)) {
			delay = new Random().nextInt(conf.getDelay());
		}
		else {
			delay = conf.getDelay();
		}
		Duration duration = Duration.ofMillis(delay);
		log.info("Makes delay in {}", duration);
		return duration;
	}

	private HttpHeaders headers(HttpHeaders incomingHeaders) {
		final HttpHeaders headers = new HttpHeaders();
		headers.putAll(incomingHeaders);
		final String host = URI.create(incomingHeaders.getFirst(FORWARDED_URL)).getHost();
		headers.put(HttpHeaders.HOST, singletonList(host));
		return headers;
	}

	public static class Conf {
		private String type;
		private int delay;

		public Conf() {
		}

		public Conf(String type, int delay) {
			this.type = type;
			this.delay = delay;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getDelay() {
			return delay;
		}

		public void setDelay(int delay) {
			if (delay <= 0 || delay > 15_000) {
				this.delay = 10_000;
			}
			else {
				this.delay = delay;
			}
		}
	}
}
