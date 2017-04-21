/**
  * Represents a tweet
  */
case class Tweet(
                tweetID: Int,
                authorID: Int,
                hashtagIDS: Set[Int],
                retweetIDS: Set[Int],
                mentionIDS: Set[Int]
                )
