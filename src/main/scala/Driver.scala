import org.apache.spark.rdd.RDD

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
  def getImportantUsers(importantHashes: RDD[Int], tweetRDD: RDD[Tweet]): RDD[Int] = {
    tweetRDD.map(tweet => (tweet.authorID, tweet.hashtagIDS))
      .map(t => t._1)
      .intersection(importantHashes)
  }

  def buildRetweetNetwork(importantUsers: RDD[Int], tweetRDD: RDD[Tweet]): RDD[(Int, Iterable[Int])] = {
    // to be used later
    val _importantUsers: RDD[(Int, (Int, Set[Int]))] = importantUsers.map(user => (user, (0, Set[Int]())))

    // filter out tweets by un-important users
    val rdd =
      tweetRDD.map(tweet => (tweet.authorID, (tweet.tweetID, tweet.retweetIDS)))
      .join(_importantUsers)
      .map(t => (t._2._1._1, t._1, t._2._1._2)) // (tweetID, authorID, retweetIDS)

    // get rdd that maps important user -> set of retweets they used
    val userRetweets =
      rdd.map(t => (t._2, t._3))
        .reduceByKey((h1, h2) => h2.union(h1))

    // get rdd that maps tweetID -> authorID
    val tweet2author = rdd.map(t => (t._1, t._2))

    // now, get rdd that maps important user -> set of authors they retweeted
    userRetweets.map(t => t._2.map(tweetID => (tweetID, t._1)))
      .flatMap(t => t.toSeq)
      .join(tweet2author)
      .map(t => (t._2._1, t._2._2))
      .groupByKey()
  }
}











