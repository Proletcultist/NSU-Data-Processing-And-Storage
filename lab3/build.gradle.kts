plugins {
    id("com.diffplug.spotless") version "8.10.4"
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    spotless{
        java{
            removeUnusedImports()
            princeOfSpace()
                .indentStyle("SPACES")
                .indentSize(4)
        }
    }
}
