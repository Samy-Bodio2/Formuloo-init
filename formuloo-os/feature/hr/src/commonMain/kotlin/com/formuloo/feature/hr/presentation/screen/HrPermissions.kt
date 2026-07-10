package com.formuloo.feature.hr.presentation.screen

/**
 * Fonctions centralisées de contrôle d'accès au module RH.
 * Les rôles proviennent de UserProfile.roles (backend Django REST).
 */

/** Accès complet : dashboard RH, liste employés, approbations, paie globale. */
fun hasHrManagerAccess(roles: List<String>): Boolean =
    roles.any {
        it.equals("rh_manager", ignoreCase = true) ||
            it.equals("admin_pme", ignoreCase = true) ||
            it.equals("super_admin", ignoreCase = true) ||
            it.equals("admin", ignoreCase = true)
    }

/** Accès aux bulletins de paie (manager, comptable, employé lui-même). */
fun hasPayslipAccess(roles: List<String>): Boolean =
    roles.any {
        it.equals("admin", ignoreCase = true) ||
            it.equals("rh_manager", ignoreCase = true) ||
            it.equals("comptable", ignoreCase = true) ||
            it.equals("employe", ignoreCase = true) ||
            it.equals("admin_pme", ignoreCase = true) ||
            it.equals("super_admin", ignoreCase = true)
    }
