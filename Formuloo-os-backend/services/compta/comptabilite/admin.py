from django.contrib import admin
from comptabilite.models import (
    Exercice, Compte, Journal, Ecriture, LigneEcriture,
    Facture, LigneFacture, Paiement,
    FactureFournisseur, LigneFactureFournisseur, PaiementFournisseur,
    Immobilisation, DotationAmortissement,
)

admin.site.register(Exercice)
admin.site.register(Compte)
admin.site.register(Journal)
admin.site.register(Ecriture)
admin.site.register(LigneEcriture)
admin.site.register(Facture)
admin.site.register(LigneFacture)
admin.site.register(Paiement)
admin.site.register(FactureFournisseur)
admin.site.register(LigneFactureFournisseur)
admin.site.register(PaiementFournisseur)
admin.site.register(Immobilisation)
admin.site.register(DotationAmortissement)
