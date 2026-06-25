## A food delivery app order simulator

### Usage

By default, the app runs in the local execution environment:

```bash
sbt run
```

You can provide runtime config with CLI args:

```bash
sbt "run --env local"
sbt "run --env staging --kafka-bootstrap-servers localhost:9092 --kafka-client-id hungry-capybara-order-simulator --kafka-topic-prefix hungry-capybara"
sbt "run --env local --initial-customer-count 1000000"
```

Or load config from YAML:

```bash
sbt "run --config app.yaml.example"
```

CLI args override YAML values when both are provided.

The app seeds 1,000,000 existing customers by default. Set `initialCustomerCount` in YAML or pass
`--initial-customer-count` to override it.

### Build A Runnable Fat Jar

To build a single runnable jar with all dependencies included:

```bash
sbt assembly
```

The jar will be created at:

```bash
target/scala-3.8.3/HungyCapybara-assembly-0.1.0-SNAPSHOT.jar
```

Run it with CLI args the same way as `sbt run`:

```bash
java -jar target/scala-3.8.3/HungyCapybara-assembly-0.1.0-SNAPSHOT.jar --env local
java -jar target/scala-3.8.3/HungyCapybara-assembly-0.1.0-SNAPSHOT.jar --config app.yaml.example
java -jar target/scala-3.8.3/HungyCapybara-assembly-0.1.0-SNAPSHOT.jar --env staging --kafka-bootstrap-servers localhost:9092 --kafka-client-id hungry-capybara-order-simulator --kafka-topic-prefix hungry-capybara
```

---

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).
