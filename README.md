# Delay Service

CF [Route Service](https://docs.cloudfoundry.org/services/route-services.htmlgi) that makes request<->response slow intentionally to emulate unstable network.


## Deploy

```
mvn clean package -DskipTests=true
cf push
cf create-user-provided-service delay-service -r https://delay-service.cfapps.io
``` 

## Bind the route service

```
cf bind-route-service cfapps.io delay-service --hostname your-app
```


## Change delay (ms)

This route service makes random delay (0 - 10,000 msec) by default.
You can change the delay by REST API

```
curl -v https://delay-service.cfapps.io -H "Content-Type: application/json" -d "{\"type\":\"random\", \"delay\":5000}"
```

If you prefer fixed delay, set `"type":"fixed"`. 

```
curl -v https://delay-service.cfapps.io -H "Content-Type: application/json"  -d "{\"type\":\"fixed\", \"delay\":5000}"
```

You can confirm current configuration

```
curl -v https://delay-service.cfapps.io
```

## Unbind the route service

```
cf unbind-route-service cfapps.io delay-service --hostname your-app
```
