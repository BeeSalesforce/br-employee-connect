# Kafka Consumer Example for Heroku Kafka using mTls

You will need to create a Heroku account and install the Heroku CLI, eg.
`brew install heroku`.

```
git clone https://github.com/rt-heroku/kafkaconsumer.git
cd kafkaconsumer

export KAFKA_URL=`heroku config:get KAFKA_URL --app <HEROKU_APP>`
export KAFKA_TOPIC=`heroku config:get KAFKA_TOPIC --app <HEROKU_APP>`
export KAFKA_TRUSTED_CERT=`heroku config:get KAFKA_TRUSTED_CERT --app <HEROKU_APP>`
export KAFKA_CLIENT_CERT_KEY=`heroku config:get KAFKA_CLIENT_CERT_KEY --app <HEROKU_APP>`
export KAFKA_CLIENT_CERT=`heroku config:get KAFKA_CLIENT_CERT --app <HEROKU_APP>`
```

For a Private Space instance of Heroku Kafka first get your public ip with the following command:
```
curl -4 icanhazip.com
```
Install the Mutual TLS Heroku CLI Plugin
```
heroku plugins:install mtls
```

And then create a route in heroku to allow connection

```
heroku data:mtls:ip-rules:create <KAFKA_INSTANCE_NAME> --app <HEROKU_APP> --cidr "your_public_ip_address/32" --description "My Public IP"
```

Wait around 10 or 15 mins for the route to be approved, in the meantime you can check it it's ready with the following command:
```
heroku data:mtls:ip-rules:create <KAFKA_INSTANCE_NAME> --app <HEROKU_APP>
```

Finally, run your the app with maven!
```
mvn clean; mvn spring-boot:run
```