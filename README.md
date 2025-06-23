# SouJava `client6` demo app

Fetch project:

```sh
git clone https://github.com/bevzzz/soujava-client6.git
cd soujava-client6/
```

Store API key in enviroment:

```sh
echo "export SOUJAVA_API_KEY=xxx" > .env
source .env
```

Run the demo:

```sh
mvn exec:java -Dexec.mainClass="com.dyma.App"
```
