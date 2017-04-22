/**
  * Represents a tweet
  */
case class Tweet(
                tweetID: Int,
                authorID: Int,
                hashtagIDS: Set[Int] = Set(),
                retweetOf: Option[Tweet] = None,
                mentionIDS: Set[Int] = Set()
                )
