---
pagetitle: Au hasard
lang: fr
document-css: false
---

## Que signifie $\mathcal{U}(a, b)$ ?

C'est la notation mathématique de la *loi uniforme* entre $a$ et $b$ : toutes les valeurs de $a$ à $b$ sont équiprobables. C'est exactement ce que fait *Au hasard*.

## Les résultats sont-ils vraiment aléatoires ?

Oui. *Au hasard* s'appuie sur deux sources d'aléa :

- Le système d'exploitation de votre téléphone. Le noyau Linux collecte des
  événements physiques imprévisibles, comme l'instant précis des interruptions
  matérielles et, sur la plupart des téléphones récents, la sortie d'un générateur
  de nombres aléatoires matériel intégré à la puce. L'application part du
  générateur de nombres aléatoires du noyau.
- Vous. Chaque fois que vous appuyez sur Tirer ou Mélanger, l'instant exact de
  votre toucher, à la nanoseconde près, est mélangé à l'aléa.

Les résultats sont imprévisibles tant que l'une des deux sources l'est. Même si le
générateur du téléphone était défaillant, ou volontairement affaibli, l'instant de
vos touchers empêcherait quiconque en dehors de votre téléphone de prédire ou de
reproduire les tirages.

Au-delà de l'imprévisibilité, un tirage équitable doit aussi être statistiquement
correct : chaque issue également probable, et chaque tirage indépendant des
précédents. Les méthodes d'*Au hasard*, décrites plus en détail ci-dessous, sont
soigneusement vérifiées par des tests statistiques : des
[tests du χ²](https://en.wikipedia.org/wiki/Pearson%27s_chi-squared_test)
d'uniformité et d'indépendance entre tirages consécutifs, et un
[test des séquences](https://en.wikipedia.org/wiki/Wald%E2%80%93Wolfowitz_runs_test)
pour les tendances.

## Comment ça marche, exactement ?

Au démarrage de l'application, une clé de 256 bits est lue dans le générateur de
nombres aléatoires du noyau, `/dev/urandom`. Avant chaque tirage ou mélange, la clé
est remplacée par le [SHA-256](https://en.wikipedia.org/wiki/SHA-2) de la clé
précédente et de l'instant de votre toucher en nanosecondes. À partir de la clé
courante, le chiffrement
[ChaCha20](https://en.wikipedia.org/wiki/Salsa20#ChaCha_variant) produit le flux
de nombres aléatoires de 64 bits dont sont faits les tirages et les mélanges
ci-dessous.

*Tirage de nombres.* Pour tirer parmi $n = b - a + 1$ nombres, l'application prend
un nombre aléatoire de 64 bits $x$ et renvoie $a + (x \bmod n)$. Fait naïvement,
cela introduirait un léger biais : les $2^{64}$ valeurs possibles de $x$ ne se
répartissent pas toujours également entre les $n$ résultats, et certains sortiraient
un tout petit peu plus souvent. L'application l'évite en écartant les
$2^{64} \bmod n$ plus petites valeurs de $x$ et en tirant de nouveau quand elle
tombe sur l'une d'elles
([échantillonnage par rejet](https://en.wikipedia.org/wiki/Rejection_sampling)).
Les valeurs restantes se répartissent alors également.

*Mélange de listes.* Les listes sont mélangées avec
l'[algorithme de Fisher–Yates](https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle),
qui remplit les positions une à une, chacune avec un élément choisi au hasard parmi
ceux qui n'ont pas encore été placés.

*Mélange de nombres.* Mélanger sur l'onglet Nombres affiche jusqu'à 100 nombres
différents de la plage, dans un ordre aléatoire. Écrire une plage énorme rien que
pour la mélanger serait impossible ; les nombres sont donc choisis avec
l'algorithme d'échantillonnage de
[Robert Floyd](https://en.wikipedia.org/wiki/Robert_W._Floyd), qui n'a besoin que
d'autant d'étapes que de nombres choisis, puis mis dans un ordre aléatoire avec
Fisher–Yates.

## Et la vie privée ?

*Au hasard* fonctionne entièrement hors ligne. L'application n'a même pas
l'autorisation d'utiliser Internet : rien de ce que vous saisissez ou tirez ne peut
quitter votre téléphone. De même, elle ne collecte ni ne transmet aucune donnée
d'analyse ou autre.

[Politique de confidentialité](https://ro-che.info/au-hasard/PRIVACY) (en anglais).
