import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

object Main {
  def main(args: Array[String] = Array()): Unit = {
    val filepath = "/media/rhenwood39/OS/6850_proj/efilter/resultsFirst100K.json"
    val conf = new SparkConf().setAppName("test").setMaster("local")
    val sc = SparkContext.getOrCreate(conf)

    val tweets: RDD[Tweet] = Driver.genRDDOfTweets(filepath, sc)
    println("Count " + tweets.count())

    val importantHashtags: RDD[String] = Driver.getImportantHashTags("MAGA", 0.005, tweets)
    print("HashTags: " + importantHashtags.collect().toSet)
    sc.stop()
  }

  main()
}
