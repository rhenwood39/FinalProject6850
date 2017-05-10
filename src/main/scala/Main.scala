import org.apache.spark.graphx.Graph
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

object Main {
  def main(args: Array[String] = Array()): Unit = {
    val dataFilePath = readLine("Specify filepath for tweet data: ")
    val outputFilePath = readLine("Specify DIRECTORY path for output :")
    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = SparkContext.getOrCreate(conf)

    val tweets: RDD[Tweet] = Driver.genRDDOfTweets(dataFilePath, sc)
    println("Count " + tweets.count())

    val users : RDD[Long] = Driver.getAllUsers(tweets, sc)
    //val retweetEdges : RDD[(Long, Long)] = Driver.allConnectionsRetweet(tweets, sc)
    //val mentionEdges : RDD[(Long ,Long)] = Driver.allConnectionsMention(tweets, sc)
    val retweetEdges: RDD[(Long, Long)] = Driver.filterByUsersRT(users, tweets, sc)
    val mentionEdges: RDD[(Long, Long)] = Driver.filterByUsersMention(users, tweets, sc)

    val retweetGraph: Graph[String, String] = Driver.buildNetwork(users, retweetEdges)
    //val mentionGraph: Graph[String, String] = Driver.buildNetwork(users, mentionEdges)

    // Driver.writeGraphToFile(retweetGraph, outputFilePath + "/retweet")
    // Driver.writeVerticesToFile(graph, filepath + "/retweet_vertices")
    Driver.writeEdgesToFile(retweetGraph,  "/retweet_edges")

    // Driver.writeGraphToFile(mentionGraph, outputFilePath + "/mention")
    // Driver.writeGraphToFile(mentionGraph, outputFilePath + "/mention_vertices")
    // Driver.writeGraphToFile(mentionGraph, outputFilePath + "/mention_edges")

    // val graph = Driver.largestConnectedComp(_graph)
    // println("conn comp: Vertices=" + graph.vertices.count() + ", Edges=" + graph.edges.count())


    // Driver.writeVerticesToFile(graph, "/media/rhenwood39/OS/6850_proj/efilter/vertices1M")
    // Driver.writeEdgesToFile(graph, "/media/rhenwood39/OS/6850_proj/efilter/edges1M")

    sc.stop()
  }

  main()
}
