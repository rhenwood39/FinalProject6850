import org.apache.spark.graphx.Graph
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

object Main {
  def main(args: Array[String] = Array()): Unit = {
    val filepath = "/media/rhenwood39/OS/6850_proj/efilter/resultsFirst1M.json"
    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = SparkContext.getOrCreate(conf)

    val tweets: RDD[Tweet] = Driver.genRDDOfTweets(filepath, sc)
    println("Count " + tweets.count())

    val importantHashtagsR: RDD[String] = Driver.getImportantHashTags("MAGA", 0.005, tweets)
    println("HashTagsR: " + importantHashtagsR.collect().toSet)

    val importantHashtagsL: RDD[String] = Driver.getImportantHashTags("MAGA", 0.005, tweets)
    println("HashTagsL: " + importantHashtagsL.collect().toSet)

    val importantHashtags: RDD[String] = importantHashtagsL.union(importantHashtagsR).distinct()

    val importantUsers: RDD[Long] = Driver.getImportantUsers(importantHashtags, tweets, sc)
    println("Users: " + importantUsers.count())

    val connections: RDD[(Long, Long)] = Driver.filterByUsersRT(importantUsers, tweets, sc)
    println("RT Edges: " + connections.count())

    val _graph: Graph[String, String] = Driver.buildNetwork(importantUsers, connections)

    val graph = Driver.largestConnectedComp(_graph)
    println("conn comp: Vertices=" + graph.vertices.count() + ", Edges=" + graph.edges.count())

    Driver.writeVerticesToFile(graph, "/media/rhenwood39/OS/6850_proj/efilter/vertices1M")
    Driver.writeEdgesToFile(graph, "/media/rhenwood39/OS/6850_proj/efilter/edges1M")

    sc.stop()
  }

  main()
}
