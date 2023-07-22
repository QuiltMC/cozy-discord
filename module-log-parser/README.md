# Log Parsing Module

This module is intended to be used by KordEx-based bots that want to provide modded Minecraft log parsing features to
their users. This allows for the extraction of information from log files for display on Discord, and the detection of
certain problems within those log files.

This module also includes support for downloading and handling logs linked from a variety of sites, including pastebin
sites. If a site you expect to work doesn't, please open an issue, or submit a Pull Request that updates
[pastebins.yml](pastebins.yml), which is automatically downloaded by this extension.

## Features

This module currently supports the following features and use-cases. While it is designed to be extended to meet any
domain-specific use-case, if you've extended it to add support for, e.g., new mod loaders or the detection of issues
not currently supported, we'd appreciate it if you could submit a Pull Request instead of keeping the changes to
yourself.

### Log Retrieval

- Retrieval from Discord message attachments
- Retrieval from links in messages, based on [pastebins.yml](pastebins.yml)

### Log Parsing

- **Fabric-specific:** Parsing the list of installed mods from the log file, along with the java version
- **Quilt-specific:** Parsing the list of installed mods from the log file, along with their metadata and the java version
- **Launcher information:** Launcher name and version for ATLauncher, MultiMC, Prism, PolyMC, Technic and TLauncher
- **Loader information:** Loader name and version for Fabric, Forge and Quilt
- **Minecraft version**

### Log Processing

- **Quilt-specific:**
  - Detection for mods that make use of internal Fabric types
  - Detection for mods that are marked incompatible on [Quilt's forum](https://forum.quiltmc.org/t/mod-incompatibility-megathread/261)
  - Detection for out-of-date installations of Quilt Loader and QSL/QFAPI
  - Detection for the presence of Fabric API or Fabric Language Kotlin
- **Piracy launcher detection (optional):** Bails when authentication fails or TLauncher is detected
- **Player IP warning:** Warns users when they upload a log containing player IP addresses
- **Problematic launcher detection (optional):** Bails when PolyMC is detected and explains why it shouldn't be used

## Setting Up

**Maven repo:** `https://maven.quiltmc.org/repository/snapshot/`
**Coordinate:** `org.quiltmc.community:module-log-parser`
**Version:** `1.0.1-SNAPSHOT`

**Note:** If you're using the PluralKit module for KordEx, you need to load it **before** you try to load this module.
Failure to do this means that it will not be used, and PluralKit messages will not be handled correctly.

```kotlin
extensions {
	// If you're using PluralKit, you _must_ load the PluralKit extension first.
	extPluralKit()

	// Simple log parser config, with our defaults
	extLogParser {
		// this.urlRegex: Regular expression used to extract links from messages; must contain one capturing group
		urlRegex = "(https?://[^\\s>]+)".toRegex(RegexOption.IGNORE_CASE)

		// this.parsers: Clear this if you don't want to use the default parsers
		parser(MyParsers())  // Call this to add your own parsers

		// this.processors: Clear this if you don't want to use the default processors
		processor(MyProcessor())  // Call this to add your own processors

		// this.retrievers: Clear this if you don't want to use the default processors
		retriever(MyRetriever())  // Call this to add your own retrievers

		// Global predicates that must return `true` for any log handler to be run
		globalPredicate { event -> true /** this: BaseLogHandler **/ }

		// Not currently implemented; use this to add checks that must pass for staff commands to run
		staffCommandCheck { hasPermission(Permission.Administrator) }

		// Not currently implemented; use this to add checks that must pass for user commands to run
		userCommandCheck { hasPermission(Permission.SendMessages) }
	}

	// With a custom type that extends `LogParserConfig`
	extLogParser(MyConfigType())
}
```

### Environment Variables

For the most part, you shouldn't need to touch these. However, a few extra settings can be configured by providing
some environment variables:

* `PASTEBIN_CONFIG_URL` - Link to an alternative [pastebins.yml](pastebins.yml) file
* `PASTEBIN_REFRESH_MINS` - How often to reload `PASTEBIN_CONFIG_URL`, in minutes (defaults to `60`)
