import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterEach, FunSuite}

class Driver$Test extends FunSuite with BeforeAndAfterEach {

  var conf: SparkConf = _
  var sc: SparkContext = _
  var tweetRDD: RDD[Tweet] = _

  override def beforeEach() {
    val t1 = Tweet(1, 1, Set(1, 3, 4), None, Set(2))
    val t2 = Tweet(2, 1, Set(3, 4, 8), None, Set(3, 5))
    val t3 = Tweet(3, 2, Set(2, 3, 4, 8), Option(t1), Set())
    val t4 = Tweet(4, 3, Set(10), Option(t1), Set(1, 2))
    val t5 = Tweet(5, 4, Set(3, 8, 10), None, Set())
    val t6 = Tweet(6, 5, Set(1, 4), Option(t2), Set(1))
    val t7 = Tweet(7, 5, Set(9, 10), Option(t5), Set(2))

    conf = new SparkConf().setAppName("test").setMaster("local")
    sc = new SparkContext(conf)

    tweetRDD = sc.parallelize(Seq(t1, t2, t3, t4, t5, t6, t7))
  }

  override def afterEach() {
    sc.stop()
  }

  test("testGetImportantHashTags") {
    var testRDD = Driver.getImportantHashTags(3, 0.5, tweetRDD)
    var testSet = testRDD.collect().toSet

    assert(testSet.equals(Set(3,4,8)))

    testRDD = Driver.getImportantHashTags(1, 0.5, tweetRDD)
    testSet = testRDD.collect().toSet

    assert(testSet.equals(Set(1,4)))
  }

  test("testGetImportantUsers") {
    var tweets = sc.parallelize(Seq(1))
    var testRDD = Driver.getImportantUsers(tweets, tweetRDD, sc)
    var testSet = testRDD.collect().toSet

    assert(testSet.equals(Set(1,5)))

    tweets = sc.parallelize(Seq(3, 4, 8))
    testRDD = Driver.getImportantUsers(tweets, tweetRDD, sc)
    testSet = testRDD.collect().toSet

    assert(testSet.equals(Set(1,2,4,5)))
  }

  test("testFilterByUsersRT") {
    var tweets = sc.parallelize(Seq(1))
    var testRDD = Driver.filterByUsersRT(tweets, tweetRDD, sc)
    var testSet = testRDD.collect().toSet

    assert(testSet.isEmpty)

    tweets = sc.parallelize(Seq(1, 5))
    testRDD = Driver.filterByUsersRT(tweets, tweetRDD, sc)
    testSet = testRDD.collect().toSet

    assert(testSet.equals(Set((1, 5))))
  }

  test("testFilterByUsersMention") {
    var tweets = sc.parallelize(Seq(1))
    var testRDD = Driver.filterByUsersMention(tweets, tweetRDD, sc)
    var testSet = testRDD.collect().toSet

    assert(testSet.isEmpty)

    tweets = sc.parallelize(Seq(1, 2, 4, 5))
    testRDD = Driver.filterByUsersMention(tweets, tweetRDD, sc)
    testSet = testRDD.collect().toSet

    assert(testSet.equals(Set((1, 2), (1, 5), (5, 1), (5, 2))))
  }

  test("testBuildNetwork") {
    val graph = Driver.buildNetwork(sc.parallelize(Seq(1,2,3,4,5)), sc.parallelize(Seq((1,2),(1,5),(4,5),(1,3))))

    val vertices = graph.vertices.map(v => v._1).collect().toSet
    assert(vertices.equals(Set(1,2,3,4,5)))

    val edges = graph.edges.map(e => (e.srcId.toLong, e.dstId.toLong)).collect().toSet
    assert(edges.equals(Set((1,2), (1,5), (4,5), (1,3))))
  }

}










