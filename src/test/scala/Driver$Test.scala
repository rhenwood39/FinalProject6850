import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterEach, FunSuite}

class Driver$Test extends FunSuite with BeforeAndAfterEach {

  var conf: SparkConf = null;
  var sc: SparkContext = null;
  var tweetRDD: RDD[Tweet] = null

  override def beforeEach() {
    val t1 = Tweet(1, 1, Set(1, 3, 4), None, Set(2, 4), Set())
    val t2 = Tweet(2, 1, Set(3, 4, 8), None, Set(3, 6), Set())
    val t3 = Tweet(3, 2, Set(2, 3, 4, 8), None, Set(1, 2, 7), Set())
    val t4 = Tweet(4, 3, Set(10), None, Set(2, 3), Set())
    val t5 = Tweet(5, 4, Set(3, 8, 10), None, Set(2, 6), Set())
    val t6 = Tweet(6, 5, Set(1, 4), None, Set(4, 6), Set())
    val t7 = Tweet(7, 5, Set(9, 10), None, Set(3, 4, 6), Set())

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

    assert(!testSet.contains(1))
    assert(!testSet.contains(2))
    assert(testSet.contains(3))
    assert(testSet.contains(4))
    assert(!testSet.contains(5))
    assert(!testSet.contains(6))
    assert(!testSet.contains(7))
    assert(testSet.contains(8))
    assert(!testSet.contains(9))
    assert(!testSet.contains(10))

    testRDD = Driver.getImportantHashTags(1, 0.5, tweetRDD)
    testSet = testRDD.collect().toSet

    assert(testSet.contains(1))
    assert(!testSet.contains(2))
    assert(!testSet.contains(3))
    assert(testSet.contains(4))
    assert(!testSet.contains(5))
    assert(!testSet.contains(6))
    assert(!testSet.contains(7))
    assert(!testSet.contains(8))
    assert(!testSet.contains(9))
    assert(!testSet.contains(10))
  }

  test("testGetImportantUsers") {
    var tweets = Set(1)
    var testRDD = Driver.getImportantUsers(tweets, tweetRDD)
    var testSet = testRDD.collect().toSet

    assert(testSet.contains(1))
    assert(!testSet.contains(2))
    assert(!testSet.contains(3))
    assert(!testSet.contains(4))
    assert(testSet.contains(5))

    tweets = Set(3, 4, 8)
    testRDD = Driver.getImportantUsers(tweets, tweetRDD)
    testSet = testRDD.collect().toSet

    assert(testSet.contains(1))
    assert(testSet.contains(2))
    assert(!testSet.contains(3))
    assert(testSet.contains(4))
    assert(testSet.contains(5))
  }

  test("testBuildRetweetNetwork") {
    var users = Seq(4)
    var userRDD = sc.parallelize(users)
    var testRDD = Driver.buildRetweetNetwork(userRDD, tweetRDD)
    var testSet = testRDD.collect().toMap
    println(testSet)

    assert(!testSet.contains(1))
    assert(!testSet.contains(2))
    assert(!testSet.contains(3))
    assert(testSet.contains(4))
    assert(!testSet.contains(5))
    assert(!testSet.contains(6))

    assert(testSet(4).isEmpty)

    users = Seq(1, 2, 3)
    userRDD = sc.parallelize(users)
    testRDD = Driver.buildRetweetNetwork(userRDD, tweetRDD)
    testSet = testRDD.collect().toMap
    println(testSet)

    assert(testSet.contains(1))
    assert(testSet.contains(2))
    assert(testSet.contains(3))
    assert(!testSet.contains(4))
    assert(!testSet.contains(5))
    assert(!testSet.contains(6))

    assert(testSet(1).toSet.contains(1))
    assert(testSet(1).toSet.contains(2))
    assert(testSet(1).toSet.contains(3))
    assert(!testSet(1).toSet.contains(4))
    assert(!testSet(1).toSet.contains(5))
    assert(!testSet(1).toSet.contains(6))

    assert(testSet(2).toSet.contains(1))
    assert(!testSet(2).toSet.contains(2))
    assert(!testSet(2).toSet.contains(3))
    assert(!testSet(2).toSet.contains(4))
    assert(!testSet(2).toSet.contains(5))
    assert(!testSet(2).toSet.contains(6))

    assert(testSet(3).toSet.contains(1))
    assert(testSet(3).toSet.contains(2))
    assert(!testSet(3).toSet.contains(3))
    assert(!testSet(3).toSet.contains(4))
    assert(!testSet(3).toSet.contains(5))
    assert(!testSet(3).toSet.contains(6))
  }

}










