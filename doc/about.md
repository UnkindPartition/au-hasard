---
pagetitle: Au hasard
lang: en
document-css: false
---

## What does *Au hasard* mean?

It's French for "at random", pronounced roughly "oh ah-ZAR" (IPA: /o a.zaʁ/).

## What does ![𝒰(a, b)](formula.svg){.formula} mean?

It's the mathematical notation for the *uniform distribution* between *a* and *b*:
every value from *a* to *b* is equally likely. That's exactly what *Au hasard* does.

## Are the results truly random?

*Au hasard* gets its randomness from your phone's operating system: the
cryptographically secure random number generator of the Linux kernel, the same
source that is used to generate encryption keys. The kernel collects unpredictable
physical events, such as the precise timing of hardware interrupts and, on most
modern phones, the output of a hardware random number generator built into the
chip. From these it seeds a generator whose output can't be predicted or
reproduced by anyone, including the author of this app.

## But random.org says these numbers are pseudo-random and «generated in a predictable fashion»!

That's true of ordinary pseudo-random number generators, like the ones built into
most programming languages: they're simple formulas, so after seeing a few outputs
you can work out all the following ones.

*Au hasard*'s numbers are pseudo-random too, but in a much stronger sense. They come
from a [*cryptographically secure*](https://en.wikipedia.org/wiki/Cryptographically_secure_pseudorandom_number_generator)
generator, which is designed so that its output
can't be predicted, even by someone who has seen any number of previous outputs,
without knowing its internal state. That state is seeded from physical events,
like random.org's numbers, is constantly refreshed with more of them, and never
leaves your phone's operating system. Predicting it would mean breaking the
cryptography that protects encryption keys, including the ones that keep your
connection to random.org private.

In practice, for drawing a number, a winner or an order, there's no difference
you could detect: both sources are unpredictable and unbiased. One thing random.org
does offer is draws recorded by a third party, so that others can check that a draw
was fair. If you need that, for example for an official lottery, use such a
service.

## What about privacy?

*Au hasard* is built to respect it:

- It works fully offline. The app doesn't even have permission to use the Internet,
  so nothing you enter or draw can leave your phone.
- No ads, no tracking, no analytics, no account.
- Your saved lists are stored only on your phone, and excluded from cloud backups.
- It's free software: anyone can read the source code and check all of the above.

## How does it work?

*The source of randomness* is Java's `SecureRandom`, which on Android is
provided by BoringSSL and seeded from the Linux kernel's random number generator.

*Drawing numbers.* To draw from a range of *n* = *b* − *a* + 1 numbers, the app
takes a random 64-bit number *x* and returns *a* + (*x* mod *n*). Done naively,
this would be slightly biased: the 2^64^ possible values of *x* can't always be
split evenly among *n* results, so some results would come up a tiny bit more
often. The app avoids this by discarding the 2^64^ mod *n* smallest values of *x*
and drawing again when it gets one of them
([rejection sampling](https://en.wikipedia.org/wiki/Rejection_sampling)). The
remaining values split evenly.

*Shuffling and picking.* Lists are shuffled with the
[Fisher–Yates algorithm](https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle), which
fills the positions one by one, each with an item chosen at random from those not
yet placed. Picking items stops after the first positions. Distinct numbers from a
range are chosen with [Robert Floyd](https://en.wikipedia.org/wiki/Robert_W._Floyd)'s
sampling algorithm, which is fast even for huge
ranges, and then shuffled.

*Testing.* The algorithms are checked with statistical tests:
[chi-square tests](https://en.wikipedia.org/wiki/Pearson%27s_chi-squared_test) of
uniformity and of independence between consecutive draws, and a
[runs test](https://en.wikipedia.org/wiki/Wald%E2%80%93Wolfowitz_runs_test). Each test
is repeated with 2,000 different seeds, and the spread of its outcomes is tested in
turn.

## Credits

*Au hasard* is free software, licensed under the
[GNU General Public License](https://www.gnu.org/licenses/gpl-3.0.html), version 3
or later.

The text is set in CMU Serif, a Unicode version of Donald Knuth's Computer Modern
converted and maintained by Andrey V. Panov, under the
[SIL Open Font License](https://openfontlicense.org), version 1.1.
