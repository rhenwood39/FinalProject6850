/**
  * Represents a tweet
  */
case class Tweet(
                tweetID: Int,
                authorID: Int,
                hashtagIDS: Set[Int],
                retweetOf: Option[Int],
                retweetIDs: Set[Int],
                mentionIDS: Set[Int]
                )
