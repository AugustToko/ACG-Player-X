package top.geek_studio.chenlongcould.musicplayer.ui

/** Also executable by a plain Kotlin/JVM compiler; no Android framework or test doubles. */
internal object ImportPreviewPreparationCases {
    val cases: List<Pair<String, () -> Unit>> = listOf(
        "document waits for metadata" to {
            val gate = newGate()
            val metadata = gate.beginMetadata()
            val request = gate.beginImport()
            check(gate.completeDocument(request, "backup"))
            check(gate.phase == StatisticsImportPhase.WAITING_FOR_METADATA)
            check(gate.nextBuild() == null)
            check(gate.completeMetadata(metadata, listOf("song")))
            val build = checkNotNull(gate.nextBuild())
            check(build.document == "backup" && build.metadata == listOf("song"))
            check(gate.completePreview(build, "preview"))
            check(gate.phase == StatisticsImportPhase.PREVIEW)
        },
        "metadata waits for document" to {
            val gate = newGate()
            val revision = gate.beginMetadata()
            val request = gate.beginImport()
            check(gate.completeMetadata(revision, listOf("song")))
            check(gate.phase == StatisticsImportPhase.READING)
            check(gate.nextBuild() == null)
            check(gate.completeDocument(request, "backup"))
            check(gate.nextBuild() != null)
        },
        "selection before first scan is retained" to {
            val gate = newGate()
            val request = gate.beginImport()
            gate.completeDocument(request, "backup")
            check(gate.phase == StatisticsImportPhase.WAITING_FOR_METADATA)
            gate.completeMetadata(gate.beginMetadata(), listOf("song"))
            check(checkNotNull(gate.nextBuild()).document == "backup")
        },
        "empty successful scan is ready" to {
            val gate = readyGate(emptyList())
            gate.completeDocument(gate.beginImport(), "offline backup")
            check(checkNotNull(gate.nextBuild()).metadata.isEmpty())
        },
        "failed scan is not an empty successful scan" to {
            val gate = newGate()
            val revision = gate.beginMetadata()
            gate.completeDocument(gate.beginImport(), "backup")
            gate.failMetadata(revision)
            check(!gate.hasMetadata)
            check(gate.phase == StatisticsImportPhase.METADATA_FAILED)
            check(gate.nextBuild() == null)
        },
        "retry retains parsed backup" to {
            val gate = newGate()
            val revision = gate.beginMetadata()
            gate.completeDocument(gate.beginImport(), "backup")
            gate.failMetadata(revision)
            val retry = gate.beginMetadata()
            gate.completeMetadata(retry, listOf("restored"))
            val input = checkNotNull(gate.nextBuild())
            check(input.document == "backup" && input.metadata == listOf("restored"))
        },
        "cancel while waiting never resurrects preview" to {
            val gate = newGate()
            val revision = gate.beginMetadata()
            gate.completeDocument(gate.beginImport(), "backup")
            gate.cancelImport()
            gate.completeMetadata(revision, listOf("song"))
            check(gate.phase == StatisticsImportPhase.NONE)
            check(gate.nextBuild() == null && gate.preview == null)
        },
        "cancel rejects late file result and error" to {
            val gate = readyGate()
            val request = gate.beginImport()
            gate.cancelImport()
            check(!gate.completeDocument(request, "late"))
            check(!gate.failDocument(request))
            check(gate.phase == StatisticsImportPhase.NONE)
        },
        "new selection supersedes old read" to {
            val gate = readyGate()
            val first = gate.beginImport()
            val second = gate.beginImport()
            check(!gate.completeDocument(first, "old"))
            check(!gate.failDocument(first))
            check(gate.completeDocument(second, "new"))
            check(checkNotNull(gate.nextBuild()).document == "new")
        },
        "stale scan success and failure are ignored" to {
            val gate = newGate()
            val old = gate.beginMetadata()
            val current = gate.beginMetadata()
            check(!gate.completeMetadata(old, listOf("old")))
            check(!gate.failMetadata(old))
            check(gate.isLoadingMetadata)
            check(gate.completeMetadata(current, listOf("new")))
            check(gate.hasMetadata)
        },
        "new scan invalidates visible preview" to {
            val gate = readyGate()
            gate.completeDocument(gate.beginImport(), "backup")
            gate.completePreview(checkNotNull(gate.nextBuild()), "old preview")
            val next = gate.beginMetadata()
            check(gate.preview == null && !gate.hasMetadata)
            check(gate.phase == StatisticsImportPhase.WAITING_FOR_METADATA)
            gate.completeMetadata(next, listOf("new identity"))
            check(checkNotNull(gate.nextBuild()).metadata == listOf("new identity"))
        },
        "old matching result cannot cross metadata revision" to {
            val gate = readyGate()
            gate.completeDocument(gate.beginImport(), "backup")
            val old = checkNotNull(gate.nextBuild())
            gate.completeMetadata(gate.beginMetadata(), listOf("new"))
            val current = checkNotNull(gate.nextBuild())
            check(!gate.completePreview(old, "stale"))
            check(!gate.failPreview(old))
            check(gate.completePreview(current, "current"))
        },
        "old matching result cannot cross import request" to {
            val gate = readyGate()
            gate.completeDocument(gate.beginImport(), "old")
            val old = checkNotNull(gate.nextBuild())
            gate.completeDocument(gate.beginImport(), "new")
            check(!gate.completePreview(old, "stale"))
            check(checkNotNull(gate.nextBuild()).document == "new")
        },
        "ready pair is claimed once" to {
            val gate = readyGate()
            gate.completeDocument(gate.beginImport(), "backup")
            val input = checkNotNull(gate.nextBuild())
            check(gate.nextBuild() == null)
            gate.completePreview(input, "preview")
            check(gate.nextBuild() == null)
            check(!gate.completePreview(input, "duplicate"))
        },
        "read failure is recoverable by selecting another file" to {
            val gate = readyGate()
            val failed = gate.beginImport()
            gate.failDocument(failed)
            check(gate.phase == StatisticsImportPhase.FAILED)
            check(!gate.completeDocument(failed, "late"))
            gate.completeDocument(gate.beginImport(), "valid")
            check(gate.nextBuild() != null)
        },
        "matching failure cannot be overwritten by late success" to {
            val gate = readyGate()
            gate.completeDocument(gate.beginImport(), "backup")
            val input = checkNotNull(gate.nextBuild())
            check(gate.failPreview(input))
            check(!gate.completePreview(input, "late"))
            check(gate.phase == StatisticsImportPhase.FAILED)
        },
    )

    private fun newGate() = ImportPreviewPreparation<String, List<String>, String>()
    private fun readyGate(songs: List<String> = listOf("song")) = newGate().apply {
        completeMetadata(beginMetadata(), songs)
    }
}

fun main() {
    ImportPreviewPreparationCases.cases.forEach { (name, run) ->
        run()
        println("PASS: $name")
    }
    println("${ImportPreviewPreparationCases.cases.size} preparation cases passed")
}
