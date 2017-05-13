
require 'json'

FILE_PATH_PREFIX = "data/"
FILE_NAME = "george.txt"
FILE_PATH = FILE_PATH_PREFIX + FILE_NAME
USER_ID_TO_HT = FILE_PATH_PREFIX + "user_id_to_hashtags.txt"
USER_ID_TO_RT = FILE_PATH_PREFIX + "user_id_to_retweet_user_ids.txt"
USER_ID_TO_REPLY = FILE_PATH_PREFIX + "user_id_to_reply_user_ids.txt"
USER_ID_TO_MENTION = FILE_PATH_PREFIX + "user_id_to_mention_user_ids.txt"

def parse_graph()
	File.readlines(FILE_PATH).each do |line|

		tweet_json = JSON.parse(line)

		if tweet_json.key?("retweeted_status")
			File.open(USER_ID_TO_RT, "a+") do |f|     
  				f.puts tweet_json["user"]["id"].to_s + " " + tweet_json["retweeted_status"]["user"]["id"].to_s
			end
		end

		if !tweet_json["in_reply_to_user_id"].nil?
			File.open(USER_ID_TO_REPLY, "a+") do |f|     
  				f.puts tweet_json["user"]["id"].to_s + " " + tweet_json["in_reply_to_user_id"].to_s
			end
		end
		
		File.open(USER_ID_TO_MENTION, "a+") do |f|
			for user_mention in tweet_json["entities"]["user_mentions"] do     
  				f.puts tweet_json["user"]["id"].to_s + " " + user_mention["id"].to_s
			end
		end

		File.open(USER_ID_TO_HT, "a+") do |f|     
  			for hashtag in tweet_json["entities"]["hashtags"] do     
  				f.puts tweet_json["user"]["id"].to_s + " " + hashtag["text"].to_s
			end  
		end
	end
end

parse_graph()