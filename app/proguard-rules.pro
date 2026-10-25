# xmlutil's serialization ServiceLoader hook isn't used since we only depend on xmlutil-core.
-dontwarn nl.adaptivity.xmlutil.util.SerializationProvider
-dontwarn nl.adaptivity.xmlutil.util.DefaultSerializationProvider

# Loaded via reflection (Class.forName / ServiceLoader), not referenced directly.
-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer { <init>(...); }
-keep class androidx.media3.decoder.midi.MidiRenderer { <init>(...); }
-keep class androidx.media3.decoder.midi.MidiExtractor { <init>(...); }
-keep class io.ktor.client.engine.cio.CIOEngineContainer { <init>(...); }
-keep class coil3.network.ktor3.internal.KtorNetworkFetcherServiceLoaderTarget { <init>(...); }
