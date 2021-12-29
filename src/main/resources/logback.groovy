import ch.qos.logback.core.joran.spi.ConsoleTarget

def environment = System.getenv().getOrDefault("ENVIRONMENT", "prod")

def defaultLevel = INFO
def defaultTarget = ConsoleTarget.SystemErr

if (environment == "dev") {
    defaultLevel = DEBUG
    defaultTarget = ConsoleTarget.SystemOut

    // Silence warning about missing native PRNG
    logger("io.ktor.util.random", ERROR)
}

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%boldGreen(%d{yyyy-MM-dd HH:mm:ss}) %boldWhite(|) %highlight(%5level) %boldWhite(|) %boldCyan(%25.25logger{25}) %boldWhite(| %msg%n)"

        withJansi = true
    }

    target = defaultTarget
}

root(defaultLevel, ["CONSOLE"])
