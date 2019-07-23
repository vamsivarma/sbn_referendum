import matplotlib.pyplot as plt
from wordcloud import WordCloud

import json
from pprint import pprint

with open('yesWords.json') as f:
    yes_words = json.load(f)

d = {}
for a in yes_words:
    d[a] = yes_words[a]


print("Word Cloud for YES words")

wordcloud = WordCloud()
wordcloud.generate_from_frequencies(frequencies=d)
plt.figure()
plt.imshow(wordcloud, interpolation="bilinear")
plt.axis("off")
plt.show()


print("Word Cloud for NO words")

with open('noWords.json') as f:
    no_words = json.load(f)

d = {}
for a in no_words:
    d[a] = no_words[a]



wordcloud = WordCloud()
wordcloud.generate_from_frequencies(frequencies=d)
plt.figure()
plt.imshow(wordcloud, interpolation="bilinear")
plt.axis("off")
plt.show()