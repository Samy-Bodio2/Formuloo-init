# Formuloo Os

**Parcours** : Backend  
**Niveau** : N5 — Master 2  
**Branche de livraison** : ta branche `prenom-nom`

## Convention de branchement — OBLIGATOIRE

```bash
git clone https://gitlab.formuloo.com/formuloo-backend/formuloo-os.git
git checkout develop
git pull origin develop
git checkout -b prenom-nom   # ex: jean-dupont, sans accents, minuscules
```

Pour chaque feature ou tâche :
```bash
# 1. Crée une sous-branche depuis ta branche
git checkout -b prenom-nom/feature/nom-feature

# 2. Travaille et commite
git add . && git commit -m "feat: description"

# 3. Pousse la sous-branche
git push origin prenom-nom/feature/nom-feature

# 4. Ouvre une Merge Request sur GitLab :
#    prenom-nom/feature/nom-feature  →  prenom-nom
#    ⚠️  PAS vers develop ni main

# 5. Merge dans ta branche et pousse
git checkout prenom-nom
git push origin prenom-nom
```

**Ta branche `prenom-nom` est ta branche de livraison — c'est ce qui sera évalué.**  
Ne jamais pousser directement sur `develop` ou `main`.
