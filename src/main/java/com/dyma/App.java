package com.dyma;

import java.io.IOException;
import java.util.List;

import io.weaviate.client6.v1.api.Authorization;
import io.weaviate.client6.v1.api.Config;
import io.weaviate.client6.v1.api.WeaviateClient;
import io.weaviate.client6.v1.api.collections.query.Metadata;
import io.weaviate.client6.v1.api.collections.query.SearchOperator;
import io.weaviate.client6.v1.api.collections.query.Where;

/**
 * Hello Weaviate!
 */
public class App {
    static final String API_KEY;
    static {
        API_KEY = System.getenv("SOUJAVA_API_KEY");
        assert !API_KEY.isBlank() : "$API_KEY is blank!";
    }

    public static void main(String[] args) {
        // ====================================================================
        // Connecting to the Weavite instance
        // ====================================================================
        try (final WeaviateClient client = new WeaviateClient(Config.of(
                "https", conn -> conn
                        .httpHost("bvsswf2qxwcrmgngqzwbw.c0.europe-west3.gcp.weaviate.cloud").httpPort(443)
                        .grpcHost("grpc-bvsswf2qxwcrmgngqzwbw.c0.europe-west3.gcp.weaviate.cloud").grpcPort(443)
                        .authorization(Authorization.apiKey(API_KEY))))) {

            final var songs = client.collections.use("EminemSongs");

            // --------------------------------------------------------------------------------------------------------

            System.out.println("""
                    \n
                    ====================================================================
                    Using keyword search to query song data
                    ====================================================================

                    ```
                    var rapGodSongs = songs.query.bm25(
                        "Rap God",
                        bm25 -> bm25
                            .queryProperties("title")
                            .searchOperator(SearchOperator.and())
                            .autocut(1)
                            .returnMetadata(Metadata.ID, Metadata.VECTOR)
                    );
                    ```
                            """);

            var rapGodSongs = songs.query.bm25(
                    "Rap God",
                    bm25 -> bm25
                            .queryProperties("title")
                            .searchOperator(SearchOperator.and())
                            .autocut(1)
                            .returnMetadata(Metadata.ID, Metadata.VECTOR));

            var titles = rapGodSongs.objects().stream().map(
                    song -> (String) song.properties().get("title")).toList();

            System.out.printf("BM25 query with autocut=1 matched %d songs:\n\t%s\n\n\n", rapGodSongs.objects().size(),
                    String.join("\n\t", titles));

            rapGodSongs = songs.query.bm25(
                    "Rap God",
                    bm25 -> bm25
                            .queryProperties("title")
                            .searchOperator(SearchOperator.and())
                            .autocut(2)
                            .returnMetadata(Metadata.ID, Metadata.VECTOR));

            titles = rapGodSongs.objects().stream().map(
                    song -> (String) song.properties().get("title")).toList();

            System.out.printf("BM25 query with autocut=2 matched %d songs:\n\t%s\n", rapGodSongs.objects().size(),
                    String.join("\n\t", titles));
            var rapGod = rapGodSongs.objects().get(0);

            // --------------------------------------------------------------------------------------------------------

            System.out.println("""
                    \n\n
                    ====================================================================
                    NearText: semantic search. Search for songs with words "cash" and "money".
                    ====================================================================

                    ```
                    var songsAboutMoney = songs.query.nearText(
                        List.of("cash", "money"),
                        query -> query
                            .limit(5)
                            .moveAway(2f, from -> from.concepts("gold"))
                            .returnMetadata(Metadata.DISTANCE)
                    ).objects();
                    ```
                            """);

            var songsAboutMoney = songs.query.nearText(
                    List.of("cash", "money"),
                    query -> query
                            .limit(5)
                            .moveAway(2f, from -> from.concepts("gold"))
                            .returnMetadata(Metadata.DISTANCE))
                    .objects();

            System.out.printf("Found %d songs about money / cash.\n", songsAboutMoney.size());

            var firstSong = songsAboutMoney.get(0);
            var firstSongLyrics = (String) firstSong.properties().get("lyrics");
            firstSongLyrics = firstSongLyrics
                    .replace("gold", "!!!GOLD!!!")
                    .replace("cash", "===CASH===")
                    .replace("money", "===MONEY===");

            System.out.printf("For example, the song \"%s\" has these lyrics:\n\n%s...\n",
                    firstSong.properties().get("title"), firstSongLyrics);

            // --------------------------------------------------------------------------------------------------------

            System.out.println("""
                    \n\n
                    ====================================================================
                    NearVector: vector similarity search. Search for songs similar to "Rap God".
                    ====================================================================

                    ```
                    var top3MostSimilar = songs.query.nearVector(
                    rapGodEmbedding,
                    query -> query
                        .certainty(.7f)
                        .limit(3)
                        .returnMetadata(Metadata.DISTANCE)
                        .where(
                            Where.and(
                                Where.property('year_released').gt(2015),
                                    Where.property('title').ne('Rap God')
                            ))
                    );
                    ```
                    """);

            var rapGodEmbedding = rapGod.metadata().vectors().getSingle("text2vecweaviate");

            var top3MostSimilar = songs.query.nearVector(
                    rapGodEmbedding,
                    query -> query
                            .certainty(.7f)
                            .limit(3)
                            .returnMetadata(Metadata.DISTANCE)
                            .where(
                                    Where.and(
                                            Where.property("year_released").gt(2015),
                                            Where.property("title").ne("Rap God"))));
            titles = top3MostSimilar.objects().stream().map(
                    song -> "%s (%d),\tdistance=%f".formatted(
                            song.properties().get("title"),
                            song.properties().get("year_released"),
                            song.metadata().distance()))
                    .toList();
            System.out.printf("The %d songs most similar to Rap God:\n\t%s", titles.size(),
                    String.join("\n\t", titles));

            System.out.println("""
                    \n\n
                    ====================================================================
                            """);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
