import json
with open('richie.json', 'rb') as f: 
    for line in f:
        tweet = json.loads(line)
        user_id = tweet['user']['id'] 
        
        if 'retweeted_status' in tweet:
            with open("retweet.txt", 'a') as w: 
                w.write("%s %s" % (tweet['retweeted_status']['user']['id'], user_id)) 
                w.write('\n')

        if tweet['in_reply_to_user_id'] is not None:
            with open("replies.txt", 'a') as w:
                w.write("%s %s" % (user_id, tweet['in_reply_to_user_id']))
                w.write('\n')

        entities = tweet['entities']

        if len(entities['user_mentions']) > 0: 
            with open("mentions.txt", 'a') as w:
                for user_mention in entities['user_mentions']:
                    w.write("%s %s" % (user_id, user_mention['id']))
                    w.write('\n')

        if len(entities['hashtags']) > 0: 
            with open("hashtags.txt", 'a') as w:
                for hashtag in entities['hashtags']:
                    w.write("%s %s" % (user_id, hashtag['text'].encode('ascii', 'ignore')))
                    w.write('\n')