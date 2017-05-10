import json
import cPickle as pickle

def loadTweets(filepath='results.First100K.json'):
    tweets = []
    with open(filepath, 'rb') as f:
        for line in f:
            tweet = json.loads(line)
            tweets.append(tweet)
    return tweets

def addToDictionary(d, key, item):
    if key not in d:
        d[key] = []
    d[key].append(item)

def extendToDictionary(d, key, items):
    if key not in d:
        d[key] = []
    d[key].extend(items)generateGraphData(tweets):


def extractDataFromTweets(tweets):
    user_id_to_hashtags = {}
    user_id_to_retweet_user_ids = {}
    user_id_to_reply_user_ids = {} 
    user_id_to_mention_user_ids = {}

    for tweet in tweets:
        user_id = tweet['user']['id'] 
        if 'retweeted_status' in tweet:
            addToDictionary(user_id_to_retweet_user_ids, user_id, tweet['retweeted_status']['user']['id'])
        
        if tweet['in_reply_to_user_id'] is not None:
            addToDictionary(user_id_to_reply_user_ids, user_id, tweet['in_reply_to_user_id'])
        
        entities = tweet['entities']
        
        if len(entities['user_mentions']) > 0: 
            extendToDictionary(user_id_to_mention_user_ids, user_id, [user_mention['id'] for user_mention in entities['user_mentions']])

        if len(entities['hashtags']) > 0: 
            extendToDictionary(user_id_to_hashtags, user_id, [hashtag['text'] for hashtag in entities['hashtags']])
    return user_id_to_hashtags, user_id_to_retweet_user_ids, user_id_to_reply_user_ids, user_id_to_mention_user_ids

tweets = loadTweets() #pass in filepath to tweets as argument...
user_id_to_hashtags, user_id_to_retweet_user_ids, user_id_to_reply_user_ids, user_id_to_mention_user_ids = extractDataFromTweets(tweets)
with open('user_id_to_hashtags.pickle', 'wb') as f:
    pickle.dump(user_id_to_hashtags, f)

with open('user_id_to_retweet_user_ids.pickle', 'wb') as f:
    pickle.dump(user_id_to_retweet_user_ids, f)

with open('user_id_to_reply_user_ids.pickle', 'wb') as f:
    pickle.dump(user_id_to_reply_user_ids, f)

with open('user_id_to_mention_user_ids.pickle', 'wb') as f:
    pickle.dump(user_id_to_mention_user_ids, f)