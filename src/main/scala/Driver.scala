import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._

/**
  * Driver file
  */
object Driver {
  /**
    * Finds what hashtags cooccured often with provided seed id
    * @param hashSeedID hashtag we want to use as seed
    * @param threshold jaccard sim threshold
    * @param tweetRDD set of all
    * @return rdd with id's of cooccurrent hashtags
    */
  def getImportantHashTags(hashSeedID: Int, threshold: Double, tweetRDD: RDD[Tweet]): RDD[Int] = {
    // get total # times each hashtag appears
    val counts: RDD[(Int, Int)] =
      tweetRDD.map(tweet => tweet.hashtagIDS)
        .flatMap(hashes => hashes.toSeq)
        .map(hash => (hash, 1))
        .reduceByKey((c1, c2) => c1 + c2)

    // filter out tweet's that don't contain seed
    val containingSeed: RDD[Set[Int]] =
      tweetRDD.map(tweet => tweet.hashtagIDS)
        .filter(hashes => hashes.contains(hashSeedID))

    // get # times seed appears
    val seedCount = containingSeed.count()

    // get # times each hashtag cooccurs with seed (ignore hashtags that never cooccurred)
    val cooccurCounts: RDD[(Int, Int)] =
      containingSeed.flatMap(hashes => hashes.toSeq)
        .map(hash => (hash, 1))
        .reduceByKey((c1, c2) => c1 + c2)

    // filter by by Jaccard Sim
    // Need |A intrsct B| / |A union B| = |A intrsct B| / (|A| + |B| - |A intrsct B|) >= threshold
    cooccurCounts.join(counts)
      .filter(t => t._2._1.toDouble / (t._2._2 + seedCount - t._2._1) >= threshold)
      .map(t => t._1)
  }

  /**
    * Get set of all users who have used one of the important hashtags
    * @param importantHashes id's of important hashtags
    * @param tweetRDD set of all tweets
    * @return rdd containing id's of users who have used these tweets
    */
  def getImportantUsers(importantHashes: RDD[Int], tweetRDD: RDD[Tweet], sc: SparkContext): RDD[Int] = {
    val importantHashesBC = sc.broadcast(importantHashes.collect().toSet)
    tweetRDD.map(tweet => (tweet.authorID, tweet.hashtagIDS))
      .filter(t => t._2.exists(importantHashesBC.value.contains))
      .map(t => t._1)
  }

  /**
    * Filter un-important people out of the graph
    * @param importantUsers users we want in network
    * @param tweetRDD set of tweets
    * @return
    */
  def filterByUsers(importantUsers: RDD[Int], tweetRDD: RDD[Tweet], sc: SparkContext): RDD[Tweet] = {
    val importantUsersBC = sc.broadcast(importantUsers.collect().toSet)

    // only keep original tweets written by important people
    // only keep retweets (1) retweeted by important people and (2) originally by important people
    tweetRDD.filter(tweet => importantUsersBC.value.contains(tweet.authorID)
          && (tweet.retweetOf.isEmpty || importantUsersBC.value.contains(tweet.retweetOf.get.authorID)))
  }

  /**
    * build retweet network
    * @param tweetRDD tweets we are using to build network
    * @return
    */
  def buildRetweetNetwork(tweetRDD: RDD[Tweet]): Graph[String, String] = {
    val nodes: RDD[(VertexId, String)] =
      tweetRDD.map(tweet => (tweet.authorID.toLong, null.asInstanceOf[String])).distinct()
    val edges: RDD[Edge[String]] =
      tweetRDD.filter(tweet => tweet.retweetOf.nonEmpty)
        .map(tweet => Edge(tweet.authorID.toLong, tweet.retweetOf.get.authorID.toLong))

    // get version of graph with edges backwards
    val revGraph: Graph[String, String] = Graph(nodes, edges)

    // reverse edges and remove duplicates
    revGraph.reverse.groupEdges((a, b) => null.asInstanceOf[String])
  }
}









