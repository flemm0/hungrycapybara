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
```

Or load config from YAML:

```bash
sbt "run --config app.yaml.example"
```

CLI args override YAML values when both are provided.

---

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).
