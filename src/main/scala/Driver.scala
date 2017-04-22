import org.apache.spark.SparkContext
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
    * Filter out tweets that won't help determine edges (according to retweets)
    * @param importantUsers users we want in network
    * @param tweetRDD set of tweets
    * @param sc
    * @return original author id -> retweet author id
    */
  def filterByUsersRT(importantUsers: RDD[Int], tweetRDD: RDD[Tweet], sc: SparkContext): RDD[(Int, Int)] = {
    val importantUsersBC = sc.broadcast(importantUsers.collect().toSet)

    // only keep tweets that were
    //   (1) retweets tweeted by important people
    //   (2) where original tweet was by an important person
    tweetRDD.filter(tweet => importantUsersBC.value.contains(tweet.authorID)
          && tweet.retweetOf.nonEmpty && importantUsersBC.value.contains(tweet.retweetOf.get.authorID))
      .map(tweet => (tweet.retweetOf.get.authorID, tweet.authorID))
  }

  /**
    * Filter out tweets that won't help detwemine edges (according to mentions)
    * @param importantUsers users we want in network
    * @param tweetRDD set of tweets
    * @param sc
    * @return author id -> mentioned user id
    */
  def filterByUsersMention(importantUsers: RDD[Int], tweetRDD: RDD[Tweet], sc: SparkContext): RDD[(Int, Int)] = {
    val importantUsersBC = sc.broadcast(importantUsers.collect().toSet)

    // only keep tweets that we
    //    (1) written by important user
    //    (2) mention an important user
    tweetRDD.map(tweet => tweet.mentionIDS.map(mentionID => (tweet.authorID, mentionID)))
      .flatMap(t => t.toSeq)
      .filter(t => importantUsersBC.value.contains(t._1) && importantUsersBC.value.contains(t._2))
  }

  /**
    * build retweet network
    * @param importantUsers users we want in network
    * @param connections edges we want to draw (should consist of users in important users)
    * @return graph of network
    */
  def buildNetwork(importantUsers: RDD[Int], connections: RDD[(Int, Int)]): Graph[String, String] = {

    val nodes: RDD[(VertexId, String)] =
      importantUsers.map(id => (id.toLong, null.asInstanceOf[String])).distinct()
    val edges: RDD[Edge[String]] = connections.map(t => Edge(t._1.toLong, t._2.toLong))

    Graph(nodes, edges).groupEdges((_,_) => null.asInstanceOf[String])
  }


}









