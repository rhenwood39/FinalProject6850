import twitter4j.{Status, TwitterObjectFactory}

/**
  * Simplified Tweet representation
  */
case class Tweet(tweetID: Long,
                authorID: Long,
                hashtags: Set[String] = Set(),
                retweetOf: Option[Tweet] = None,
                mentionIDS: Set[Long] = Set())

object Tweet {
//  def apply(tweetID: Long,
//            authorID: Long,
//            hashtags: Set[String] = Set(),
//            retweetOf: Option[Tweet] = None,
//            mentionIDS: Set[Long] = Set()): Tweet = new Tweet(tweetID, authorID, hashtags, retweetOf, mentionIDS)


  def apply(jsonString: String): Tweet = {
    val fullTweet: Status = TwitterObjectFactory.createStatus(jsonString)
    apply(fullTweet)
  }

  def apply(fullTweet: Status): Tweet = {
    val tweetID: Long = fullTweet.getId
    val authorID: Long = fullTweet.getUser.getId

    val _hashtags = fullTweet.getHashtagEntities.toSet
    val hashtags: Set[String] = _hashtags.map(hashtag => hashtag.getText)

    val retweetOf: Option[Tweet] = if (!fullTweet.isRetweet) None else Some(apply(fullTweet.getRetweetedStatus))

    val _mentionIDS = fullTweet.getUserMentionEntities.toSet
    val mentionIDS: Set[Long] = _mentionIDS.map(mention => mention.getId)

    new Tweet(tweetID, authorID, hashtags, retweetOf, mentionIDS)
  }
}