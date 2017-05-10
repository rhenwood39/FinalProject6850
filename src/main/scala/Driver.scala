import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._

import scala.io.Source
import scala.util.Random

/**
  * Driver file
  */
object Driver {
  /**
    * Read tweets from provided file and store in RDD
    * @param filepath
    * @param sc
    * @return rdd of tweets
    */
  def genRDDOfTweets(filepath: String, sc: SparkContext): RDD[Tweet] = {
    println("**********************************************")
    println("in gen rdd of tweets")
    var seq: Seq[Tweet] = Seq()
    var rdd: RDD[Tweet] = sc.parallelize(Seq())
    var seqRDD: Seq[RDD[Tweet]] = Seq()
    var i = 0
    var nUnions = 0
    for (line <- Source.fromFile(filepath).getLines()) {
      try {
        seq = seq ++ Seq(Tweet(line))
        if (i == 10000) {
          // print progress
          println("****************************************")
          println("union " + nUnions)
          nUnions += 1

          // update
          seqRDD = seqRDD ++ Seq(sc.parallelize(seq))
          i = 0
          seq = Seq()
        }
        i = i + 1
      } catch {
        case e => println("ERROR: " + line + "\n" + e.printStackTrace())
      }
    }
    seqRDD = seqRDD ++ Seq(sc.parallelize(seq))
    sc.union(seqRDD)
  }

  /**
    * Read tweets from provided file and store in Seq
    * @param filepath path to file we want to read
    * @return Seq of Tweet objects
    */
  def genSeqOfTweets(filepath: String): Seq[Tweet] = {
    println("**********************************************")
    println("in gen seq of tweets")
    var seq: Seq[Tweet] = Seq()
    for (line <- Source.fromFile(filepath).getLines()) {
      seq = seq ++ Seq(Tweet(line))
    }
    seq
  }

  //TODO: write genRDDOfTweets method

  /**
    * Finds what hashtags cooccured often with provided seed id
    * @param hashSeed hashtag we want to use as seed
    * @param threshold jaccard sim threshold
    * @param tweetRDD set of all
    * @return rdd with id's of cooccurrent hashtags
    */
  def getImportantHashTags(hashSeed: String, threshold: Double, tweetRDD: RDD[Tweet]): RDD[String] = {
    // get total # times each hashtag appears
    println("**********************************************")
    println("get important hashtags")
    val counts: RDD[(String, Long)] =
      tweetRDD.map(tweet => tweet.hashtags)
        .flatMap(hashes => hashes.toSeq)
        .map(hash => (hash, 1L))
        .reduceByKey((c1, c2) => c1 + c2)

    // filter out tweet's that don't contain seed
    val containingSeed: RDD[Set[String]] =
      tweetRDD.map(tweet => tweet.hashtags)
        .filter(hashes => hashes.contains(hashSeed))

    // get # times seed appears
    val seedCount = containingSeed.count()

    // get # times each hashtag cooccurs with seed (ignore hashtags that never cooccurred)
    val cooccurCounts: RDD[(String, Long)] =
      containingSeed.flatMap(hashes => hashes.toSeq)
        .map(hash => (hash, 1L))
        .reduceByKey((c1, c2) => c1 + c2)

    // filter by by Jaccard Sim
    // Need |A Longrsct B| / |A union B| = |A Longrsct B| / (|A| + |B| - |A Longrsct B|) >= threshold
    cooccurCounts.join(counts)
      .filter(t => t._2._1.toDouble / (t._2._2 + seedCount - t._2._1) >= threshold)
      .map(t => t._1)
  }

/**
  * Get id's of all users in dataset
  * @param tweetRDD set of all tweets
  * @param sc
  * @return rdd containing all user ids
import scala.io.Source
    */
  def getAllUsers(tweetRDD: RDD[Tweet], sc: SparkContext): RDD[Long] = {
    tweetRDD.map(tweet => tweet.authorID).distinct
  }

  /**
    * Get set of all users who have used one of the important hashtags
    * @param importantHashes set of important hashtags
    * @param tweetRDD set of all tweets
    * @return rdd containing id's of users who have used these tweets
    */
  def getImportantUsers(importantHashes: RDD[String], tweetRDD: RDD[Tweet], sc: SparkContext): RDD[Long] = {
    println("**********************************************")
    println("get important users")
    val importantHashesBC = sc.broadcast(importantHashes.collect().toSet)
    tweetRDD.map(tweet => (tweet.authorID, tweet.hashtags))
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
  def filterByUsersRT(importantUsers: RDD[Long], tweetRDD: RDD[Tweet], sc: SparkContext): RDD[(Long, Long)] = {
    println("**********************************************")
    println("filter by user rt")
    val importantUsersBC = sc.broadcast(importantUsers.collect().toSet)

    // only keep tweets that were
    //   (1) retweets tweeted by important people
    //   (2) where original tweet was by an important person
    tweetRDD.filter(tweet => importantUsersBC.value.contains(tweet.authorID)
          && tweet.retweetOf.nonEmpty && importantUsersBC.value.contains(tweet.retweetOf.get.authorID))
      .map(tweet => (tweet.retweetOf.get.authorID, tweet.authorID))
  }

  /**
    * Build connections based on retweets (no filtering)
    * @param tweetRDD
    * @param sc
    * @return
    */
  def allConnectionsRetweet(tweetRDD: RDD[Tweet], sc: SparkContext): RDD[(Long, Long)] = {
    tweetRDD.filter(tweet => tweet.retweetOf.nonEmpty).map(tweet => (tweet.retweetOf.get.authorID, tweet.authorID))
  }

  /**
    * Filter out tweets that won't help detwemine edges (according to mentions)
    * @param importantUsers users we want in network
    * @param tweetRDD set of tweets
    * @param sc
    * @return author id -> mentioned user id
    */
  def filterByUsersMention(importantUsers: RDD[Long], tweetRDD: RDD[Tweet], sc: SparkContext): RDD[(Long, Long)] = {
    println("**********************************************")
    println("filter by user mention")
    val importantUsersBC = sc.broadcast(importantUsers.collect().toSet)

    // only keep tweets that we
    //    (1) written by important user
    //    (2) mention an important user
    tweetRDD.map(tweet => tweet.mentionIDS.map(mentionID => (tweet.authorID, mentionID)))
      .flatMap(t => t.toSeq)
      .filter(t => importantUsersBC.value.contains(t._1) && importantUsersBC.value.contains(t._2))
  }

  /**
    * Connections based on mentions (no filter)
    * @param tweetRDD
    * @param sc
    * @return
    */
  def allConnectionsMention(tweetRDD: RDD[Tweet], sc: SparkContext): RDD[(Long, Long)] = {
    tweetRDD.map(tweet => tweet.mentionIDS.map(mentionID => (tweet.authorID, mentionID)))
      .flatMap(t => t.toSeq)
  }

  /**
    * build retweet network
    * @param importantUsers users we want in network
    * @param connections edges we want to draw (should consist of users in important users)
    * @return graph of network
    */
  def buildNetwork(importantUsers: RDD[Long], connections: RDD[(Long, Long)]): Graph[String, String] = {
    println("**********************************************")
    println("build network")
    val nodes: RDD[(VertexId, String)] =
      importantUsers.map(id => (id.toLong, null.asInstanceOf[String])).distinct()
    val edges: RDD[Edge[String]] = connections.map(t => Edge(t._1, t._2))

    Graph(nodes, edges).groupEdges((_,_) => null.asInstanceOf[String])
  }

  /**
    * Find largest connected component of graph
    * @param graph
    * @return largest connected component (note meta-data associated with verices is erosed)
    */
  def largestConnectedComp(graph: Graph[_,_]) : Graph[_, _] = {
    println("**********************************************")
    println("largest conn comp")
    val connComp = graph.connectedComponents()
    val seedID: VertexId = connComp.vertices
      .map(v => (v._2, 1L))
      .reduceByKey((c1, c2) => c1 + c2)
      .max()(new Ordering[Tuple2[VertexId, Long]]() {
        override def compare(x: (VertexId, Long), y: (VertexId, Long)): Int =
        Ordering[Long].compare(x._2, y._2)
      })._1

    connComp.subgraph(e => true, (v,d) => d == seedID)
  }

  /**
    * Perform label propagation
    * @param graph graph to do propagation on
    * @param numIters number of iterations to do
    * @return graph where each vertex is given its cluster's label
    */
  def labelProp(graph: Graph[_, _], numIters: Int): Graph[Long, _] = {
    val rand: Random = Random

    val vertices: RDD[(VertexId, Long)] = graph.vertices.map(v => (v._1, v._1))
    var _graph: Graph[Long, _] = Graph(vertices, graph.edges)

//    var prevg: Graph[Long, _] = null
    var i = 0
//    var active = 0L
    do {
//      prevg = _graph
      _graph = labelPropHelper(_graph).cache()
//      active = _graph.vertices.count()

//      prevg.unpersistVertices(blocking = false)
//      prevg.unpersistVertices(blocking = false)
      i += 1
//      if (i % 10 == 0) {
//        _graph.checkpoint()
//        //println("~~~~~~~~~~~~~~~~~~~~~~" + _graph.vertices.count())
//      }
    } while (i <= numIters)// && !isSettled(_graph))
    _graph
  }

  /**
    * return mode of list (break ties randomly)
    * @return
    */
  def mode: Iterable[Long] => Long =
    itrbl => Random.shuffle(itrbl.groupBy(i => i).mapValues(_.size)).maxBy(_._2)._1

  /**
    * checks if a value is the mode of a list
    * @return
    */
  def isMode: (Long, Iterable[Long]) => Boolean = {
    case (value, itrbl) => itrbl.groupBy(i => i).mapValues(_.size).maxBy(_._2)._2 == itrbl.count(_ == value)
  }

  /**
    * checks whether each node's label is equal to the most frequent of its neighbors' labels
    * @param graph
    * @return
    */
  def isSettled(graph: Graph[Long, _]) : Boolean = {
    val vertices: RDD[(VertexId, Long)] = graph.vertices.map(v => (v._1, v._2))
    val edges: RDD[(VertexId, VertexId)] = graph.edges.map(e => (e.srcId, e.dstId))

    vertices.join(edges) // (src, (lbl, dst))
      .map(t => (t._2._2, t._2._1)) // (dst, incoming lbl)
      .groupByKey() // (dst, List<incoming labels>)
      .join(vertices) // (dst, (inc labels, curr label))
      .map(t => isMode(t._2._2, t._2._1))
      .reduce((a, b) => a && b)
  }

  /**
    * relabels nodes at time t+1 to have most frequent label of neighbors at time t
    * @param graph
    * @return
    */
  def labelPropHelper(graph: Graph[Long, _]) : Graph[Long, _] = {
    val rand: Random = Random

    val vertices: RDD[(VertexId, Long)] = graph.vertices.map(v => (v._1, v._2))
    val edges: RDD[(VertexId, VertexId)] = graph.edges.map(e => (e.srcId, e.dstId))

    val newVertices = vertices.join(edges) // (src, (lbl, dst))
      .map(t => (t._2._2, t._2._1)) // (dst, incoming lbl)
      .groupByKey() // (dst List<incoming labels>)
      .map(t => (t._1, mode(t._2))) // (dst, new lbl)
      .rightOuterJoin(vertices) // (src, (Option<new label>, old label))
      .map(t => (t._1, t._2._1.getOrElse(t._2._2)))

    Graph(newVertices, graph.edges)
  }

  /**
    * write the vertices and edges of graph to file
    * Specifically, vertices are written to filepath + "_vertices"
    * and edges to filepath + "_edges"
    * @param graph
    * @param filepath
    * @return
    */
  def writeGraphToFile(graph: Graph[_,_], filepath: String) : Unit = {
    println("*********************")
    println("writing graph to file...")
    writeVerticesToFile(graph, filepath + "_vertices")
    writeEdgesToFile(graph, filepath + "_edges")
  }

  /**
    * Write vertices to file
    * @param graph
    * @param filepath
    */
  def writeVerticesToFile(graph: Graph[_,_], filepath: String): Unit = {
    println("**********************************************")
    println("in write vertices to file")
    graph.vertices.map(v => v._1).saveAsTextFile(filepath)
  }

  /**
    * Write vertices to file in format "vertexID, label"
    * @param graph
    * @param filepath
    */
  def writeLabeledVerticesToFile(graph: Graph[_,_], filepath: String): Unit = {
    println("**********************************************")
    println("write labels to file")
    graph.vertices.map(v => v._1 + ", " + v._2).saveAsTextFile(filepath)
  }

  /**
    * Write edges to file in format "srcNodeID, dstNodeID"
    * @param graph
    * @param filepath
    */
  def writeEdgesToFile(graph: Graph[_,_], filepath: String): Unit = {
    println("**********************************************")
    println("write edges to file")
    class customTuple[K, V](k: K, v: V) {
      override def toString: String = k.toString + " " + v.toString
    }
    graph.edges.map(e => new customTuple(e.srcId, e.dstId)).saveAsTextFile(filepath)
  }
}









