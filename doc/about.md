---
pagetitle: Au hasard
lang: en
document-css: false
---

## What does *Au hasard* mean?

It's French for "at random", pronounced "oh ah-ZAR" (/o a.zaʁ/).

## What does $\mathcal{U}(a, b)$ mean?

It's the mathematical notation for the *uniform distribution* between $a$ and $b$:
every value from $a$ to $b$ is equally likely. That's exactly what *Au hasard* does.

## Are the results truly random?

Yes. *Au hasard* uses two sources of randomness:

- Your phone's operating system. The Linux kernel collects unpredictable physical
  events, such as the precise timing of hardware interrupts and, on most modern
  phones, the output of a hardware random number generator built into the chip.
  The app takes its starting point from the kernel's random number generator.
- You. Each time you press Draw or Shuffle, the precise moment of your tap, down
  to the nanosecond, is mixed in.

The results are unpredictable as long as either source is. Even if the phone's
generator were flawed, or deliberately weakened, the timing of your taps would
keep the draws from being predicted or reproduced by anyone outside your phone.

Apart from being unpredictable, fair draws also need to be statistically correct:
every outcome equally likely, and each draw independent of the ones before it.
*Au hasard*'s methods, described below in more detail, are thoroughly verified with statistical tests:
[chi-square tests](https://en.wikipedia.org/wiki/Pearson%27s_chi-squared_test) of
uniformity and of independence between consecutive draws, and a
[runs test](https://en.wikipedia.org/wiki/Wald%E2%80%93Wolfowitz_runs_test) for
trends.

## How does it work, exactly?

When the app starts, a 256-bit key is read from the kernel's random number generator,
`/dev/urandom`. Before each draw or shuffle, the key is replaced with the
[SHA-256](https://en.wikipedia.org/wiki/SHA-2) hash of the previous key and the
time of your tap in nanoseconds. From the current key, the cipher
[ChaCha20](https://en.wikipedia.org/wiki/Salsa20#ChaCha_variant) produces the stream of random 64-bit numbers from which the draws and shuffles
below are made.

*Drawing numbers.* To draw from a range of $n = b - a + 1$ numbers, the app
takes a random 64-bit number $x$ and returns $a + (x \bmod n)$. Done naively,
this would be slightly biased: the $2^{64}$ possible values of $x$ can't always be
split evenly among $n$ results, so some results would come up a tiny bit more
often. The app avoids this by discarding the $2^{64} \bmod n$ smallest values of $x$
and drawing again when it gets one of them
([rejection sampling](https://en.wikipedia.org/wiki/Rejection_sampling)). The
remaining values split evenly.

*Shuffling lists.* Lists are shuffled with the
[Fisher–Yates algorithm](https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle), which
fills the positions one by one, each with an item chosen at random from those not
yet placed. Picking items stops after the first positions.

*Shuffling numbers.* Shuffle on the Numbers tab shows up to 100 different numbers
from the range, in random order. Writing out a huge range just to shuffle it would
be impossible, so the numbers are chosen with
[Robert Floyd](https://en.wikipedia.org/wiki/Robert_W._Floyd)'s sampling algorithm,
which needs only as many steps as numbers chosen, and then put in random order with
Fisher–Yates.

## What about privacy?

*Au hasard* works fully offline. The app doesn't even have permission to use
the Internet, so nothing you enter or draw can leave your phone. Likewise, it
doesn't collect or transmit any analytics or other data.
