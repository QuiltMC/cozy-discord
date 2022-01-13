# Cozy: Discord

This repository contains a Discord bot that we make use of to help keep the Quilt community servers running smoothly.
Its features include, but are not limited to:

* A fully-featured suggestions system, with PluralKit support
* A thread ownership system that allows users to manage and transfer their own threads
* Moderation tools, such as server and channel locking, mute role permissions syncing, adding staff to threads, and message logging
* GitHub repository management tools
* A robust message alerting and filtering system
* Minecraft snapshot alerting and tracking tools
* Cross-server ban synchronisation tools
* Miscellaneous utilities

Most of the features currently implemented within Cozy were designed with Quilt in mind, and haven't been factored out
into reusable modules. We do plan to do this at some point, but there's a ways to go yet!

Functionality is split into several modes:

* `dev`: Development server tooling, including GitHub management
* `collab`: Quilt Community Collab mode, mostly for ban-sharing
* `quilt` (default): General community management and user-facing tools
* `showcase`: Showcase mode, for allowing servers to cross-post their mod screenshots, updates and releases

Modes are specified via the `MODE` environment variable - see below for more information on that.

# Development Requirements

If you're here to help out, here's what you'll need. Firstly:

* A JDK, **Java 15 or later** - if you need one, try [Adoptium](https://adoptium.net/)
* An IDE suitable for Kotlin **and Gradle** work
  * [IntelliJ IDEA](https://www.jetbrains.com/idea/): Community Edition should be plenty
  * [Eclipse](https://www.eclipse.org/ide/): Install the latest version of [the Kotlin plugin](https://marketplace.eclipse.org/content/kotlin-plugin-eclipse), then go to the `Window` menu, `Preferences`, `Kotlin`, `Compiler` and make sure you set up the `JDK_HOME` and JVM target version
* A MongoDB server: [Download](https://www.mongodb.com/try/download/community) and install | [Docker](https://hub.docker.com/_/mongo) | [Hosted](https://www.mongodb.com/atlas/database) (there's a free tier)
  * You may also want [MongoDB Compass](https://www.mongodb.com/products/compass) if you're doing database-related work
* A Discord bot application, created at [the developer dashboard](https://discord.com/developers/applications). Make sure you turn on all the privileged intents - different modes require different intents!

# Setting Up

As a first step, fork this repository, clone your fork, and open it in your IDE, importing the Gradle project. Create
a file named `.env` in the project root (next to files like the `build.gradle.kts`), and fill it out with your bot's
settings. This file should contain `KEY=value` pairs, without a space around the `=` and without added quotes:

```dotenv
TOKEN=AAA....
DB_URL=mongodb://localhost:27017/

ENVIRONMENT=dev
# You get the idea.
```

**Required settings:**
* `TOKEN`: Your Discord bot token, which you can get from the developer dashboard linked above
* `DB_URL`: MongoDB database URL- for a local server, you might use `mongodb://localhost:27017/` for example

**Logging settings:**
* `ENVIRONMENT`: `prod` (default) for info logging on SystemErr, `dev` for debug logging on SystemOut
 
**Settings used by all modes:**
* `COMMUNITY_GUILD_ID`: ID of your "community" server
* `TOOLCHAIN_GUILD_ID`: ID of your "toolchain" server
* `GUILDS`: A comma-separated list of guild IDs, if not just the two above
* `COMMUNITY_MODERATOR_ROLE`: ID of your "toolchain moderator" role
* `TOOLCHAIN_MODERATOR_ROLE`: ID of your "community moderator" role
* `MODERATOR_ROLES`: A comma-separated list of moderator role IDs, if not just the two above

**Settings used by mode:** `quilt`
* `SUGGESTION_CHANNEL_ID`: ID of the channel to use for the suggestions system
* `MESSAGE_LOG_CATEGORIES`: A comma-separated list of category IDs to use for message logging

**Settings used by mode:** `dev`
* `GITHUB_TOKEN`: GitHub auth token, for the GitHub project management commands

Once you've filled out your `.env` file, you can use the `run` gradle task to launch the bot. If this is your first
run, you'll want to start with the `quilt` mode as this is the mode that runs the database migrations. After that,
feel free to set up and test whichever mode you need to work with.

# Conventions and Linting

This repository makes use of [detekt](https://detekt.github.io/detekt/), a static analysis tool for Kotlin code. Our
formatting rules are contained within [detekt.yml](detekt.yml), but detekt can't verify everything.

To be specific, proper spacing is important for code readability. If your code is too dense, then we're going to ask
you to fix this problem - so try to bear it in mind. Let's see some examples...

### Bad

```kotlin
override suspend fun unload() {
    super.unload()
    if (::task.isInitialized) { task.cancel() }
}
```

```kotlin
action {
    val channel = channel.asChannel() as ThreadChannel
    val member = user.asMember(guild!!.id)
    val roles = member.roles.toList().map { it.id }
    if (MODERATOR_ROLES.any { it in roles }) {
        targetMessages.forEach { it.pin("Pinned by ${member.tag}") }
        edit { content = "Messages pinned." }
        return@action
    }
    if (channel.ownerId != user.id && threads.isOwner(channel, user) != true) {
        respond { content = "**Error:** This is not your thread." }
        return@action
    }
    targetMessages.forEach { it.pin("Pinned by ${member.tag}") }
    edit { content = "Messages pinned." }
}
```

```kotlin
action {
    if (this.member?.asMemberOrNull()?.mayManageRole(arguments.role) == true) {
        arguments.targetUser.removeRole(arguments.role.id,
            "${this.user.asUserOrNull()?.tag ?: this.user.id} used /team remove"
        )
        respond {
            content = "Successfully removed ${arguments.targetUser.mention} from " +
                    "${arguments.role.mention}."
            allowedMentions { }
        }
    } else {
        respond {
            content = "Your team needs to be above ${arguments.role.mention} in order to remove " +
                    "anyone from it."
            allowedMentions { }
        }
    }
}
```

### Good

```kotlin
override suspend fun unload() {
    super.unload()

    if (::task.isInitialized) {
        task.cancel()
    }
}
```

```kotlin
action {
    val channel = channel.asChannel() as ThreadChannel
    val member = user.asMember(guild!!.id)
    val roles = member.roles.toList().map { it.id }

    if (MODERATOR_ROLES.any { it in roles }) {
        targetMessages.forEach { it.pin("Pinned by ${member.tag}") }
        edit { content = "Messages pinned." }

        return@action
    }

    if (channel.ownerId != user.id && threads.isOwner(channel, user) != true) {
        respond { content = "**Error:** This is not your thread." }

        return@action
    }

    targetMessages.forEach { it.pin("Pinned by ${member.tag}") }

    edit { content = "Messages pinned." }
}
```

```kotlin
action {
    if (this.member?.asMemberOrNull()?.mayManageRole(arguments.role) == true) {
        arguments.targetUser.removeRole(
            arguments.role.id,
      
            "${this.user.asUserOrNull()?.tag ?: this.user.id} used /team remove"
        )
    
        respond {
            content = "Successfully removed ${arguments.targetUser.mention} from " +
                    "${arguments.role.mention}."
      
            allowedMentions { }
        }
    } else {
        respond {
            content = "Your team needs to be above ${arguments.role.mention} in order to remove " +
                    "anyone from it."
      
            allowedMentions { }
        }
    }
}
```

Hopefully these examples help to make things clearer. Group similar types of statements together (variable assignments),
separating them from other types (like function calls). If a statement takes up multiple lines, then it probably needs
to be separated from any other statements. In general, use your best judgement - extra space is better than not enough
space, and detekt will tell you if you go overboard.
