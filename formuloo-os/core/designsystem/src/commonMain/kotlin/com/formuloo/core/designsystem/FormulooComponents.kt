package com.formuloo.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.common.model.ActivityIconType
import com.formuloo.core.common.model.ActivityItem
import com.formuloo.core.common.model.KpiCard
import com.formuloo.core.common.model.KpiIconType
import com.formuloo.core.common.model.ModuleIconType
import com.formuloo.core.common.model.ModuleItem
import com.formuloo.core.common.model.PendingRequest

/**
 * Composants partages du design system Formuloo OS.
 * Couleurs/typo/shapes : voir Color.kt, Typography.kt, Theme.kt.
 */

@Composable
fun FormulooButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    showArrow: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FormulooPrimary,
            contentColor = FormulooOnPrimary,
            disabledContainerColor = FormulooPrimary.copy(alpha = 0.5f),
            disabledContentColor = FormulooOnPrimary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = FormulooOnPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge)
                if (showArrow) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FormulooOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FormulooOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FormulooTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = if (label.isNotBlank()) ({ Text(label) }) else null,
            placeholder = placeholder?.let { { Text(it, color = FormulooOnSurfaceVariant) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FormulooPrimary,
                unfocusedBorderColor = FormulooOutline,
                errorBorderColor = FormulooError,
                focusedContainerColor = FormulooSurface,
                unfocusedContainerColor = FormulooSurface,
                errorContainerColor = FormulooSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = FormulooError,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun FormulooPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    FormulooTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                if (visible) VisibilityOffIcon() else VisibilityIcon()
            }
        },
        isError = isError,
        errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    )
}

@Composable
fun FormulooCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onCheckedChange(!checked) },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = FormulooPrimary),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SecureConnectionBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "secure-dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "secure-dot-alpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FormulooMint)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(FormulooPrimary.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(6.dp))
        Text("Connexion sécurisée", style = MaterialTheme.typography.labelSmall, color = FormulooPrimary)
    }
}

@Composable
fun NotificationBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(FormulooSecondary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun StepProgressBar(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < currentStep) FormulooPrimary else FormulooOutline),
            )
        }
    }
}

@Composable
fun PasswordStrengthIndicator(password: String, modifier: Modifier = Modifier) {
    val criteria = listOf(
        "12 caractères minimum" to (password.length >= 12),
        "Un chiffre" to password.any { it.isDigit() },
        "Majuscule et minuscule" to (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }),
        "Un caractère spécial" to password.any { !it.isLetterOrDigit() },
    )
    val score = criteria.count { it.second }
    val (color, label) = when {
        password.isEmpty() -> FormulooOutline to ""
        score <= 1 -> FormulooError to "Faible"
        score <= 3 -> FormulooSecondary to "Moyen"
        else -> FormulooSuccess to "Fort"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < score) color else FormulooOutline),
                )
            }
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
        Spacer(Modifier.height(8.dp))
        criteria.forEach { (text, met) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Icon(
                    imageVector = if (met) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (met) FormulooSuccess else FormulooOnSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (met) MaterialTheme.colorScheme.onSurface else FormulooOnSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Saisie d'un code a usage unique sous forme de cases individuelles, avec
 * avancement automatique du focus et clavier numerique.
 */
@Composable
fun OtpInputField(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    val focusRequesters = remember(length) { List(length) { FocusRequester() } }

    LaunchedEffect(Unit) {
        focusRequesters.firstOrNull()?.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(length) { index ->
            val digit = code.getOrNull(index)?.toString().orEmpty()
            val filled = digit.isNotEmpty()
            OutlinedTextField(
                value = digit,
                onValueChange = { input ->
                    when {
                        input.isEmpty() && index < code.length -> {
                            onCodeChange(code.removeRange(index, index + 1))
                            if (index > 0) focusRequesters[index - 1].requestFocus()
                        }
                        input.isNotEmpty() && input.last().isDigit() -> {
                            val digitChar = input.last()
                            val newCode = when {
                                index < code.length -> code.substring(0, index) + digitChar + code.substring(index + 1)
                                index == code.length -> code + digitChar
                                else -> code
                            }
                            onCodeChange(newCode.take(length))
                            if (index < length - 1) focusRequesters[index + 1].requestFocus()
                        }
                    }
                },
                modifier = Modifier
                    .width(48.dp)
                    .height(56.dp)
                    .focusRequester(focusRequesters[index]),
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FormulooPrimary,
                    unfocusedBorderColor = FormulooOutline,
                    focusedContainerColor = if (filled) FormulooMint else FormulooSurface,
                    unfocusedContainerColor = if (filled) FormulooMint else FormulooSurface,
                    focusedTextColor = FormulooPrimary,
                    unfocusedTextColor = if (filled) FormulooPrimary else MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

/**
 * Champ de selection (liste deroulante) base sur ExposedDropdownMenuBox,
 * utilise pour les listes fermees (secteur, pays, question secrete...).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulooDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = if (label.isNotBlank()) ({ Text(label) }) else null,
                isError = isError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FormulooPrimary,
                    unfocusedBorderColor = FormulooOutline,
                    errorBorderColor = FormulooError,
                    focusedContainerColor = FormulooSurface,
                    unfocusedContainerColor = FormulooSurface,
                    errorContainerColor = FormulooSurface,
                ),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = FormulooError,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

/**
 * Composants du tableau de bord (HomeScreen / AppDrawer de feature/dashboard).
 */

@Composable
fun InitialsAvatar(
    initials: String,
    size: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
        )
    }
}

@Composable
fun OrganizationCard(
    initials: String,
    name: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(initials = initials, size = 44.dp, backgroundColor = FormulooPrimary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
                Text(subtitle, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
            }
        }
    }
}

@Composable
fun TrendBadge(trend: String, positive: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = if (positive) FormulooMint else FormulooErrorBg
    val contentColor = if (positive) FormulooPrimary else FormulooError
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = trend,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun kpiIconBackground(type: KpiIconType): Color = when (type) {
    KpiIconType.PEOPLE, KpiIconType.CHART -> FormulooMint
    KpiIconType.MONEY, KpiIconType.TARGET -> FormulooSecondaryBg
}

private fun kpiIconTint(type: KpiIconType): Color = when (type) {
    KpiIconType.PEOPLE, KpiIconType.CHART -> FormulooPrimary
    KpiIconType.MONEY, KpiIconType.TARGET -> FormulooSecondary
}

@Composable
private fun KpiTypeIcon(type: KpiIconType, tint: Color) {
    when (type) {
        KpiIconType.PEOPLE -> PeopleIcon(tint = tint)
        KpiIconType.MONEY -> Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = tint)
        KpiIconType.CHART -> TrendingUpIcon(tint = tint)
        KpiIconType.TARGET -> TargetIcon(tint = tint)
    }
}

@Composable
fun KpiCardItem(kpi: KpiCard, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(kpiIconBackground(kpi.iconType)),
                    contentAlignment = Alignment.Center,
                ) {
                    KpiTypeIcon(type = kpi.iconType, tint = kpiIconTint(kpi.iconType))
                }
                TrendBadge(trend = kpi.trend, positive = kpi.trendPositive)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = kpi.value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = FormulooTextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = kpi.label,
                fontSize = 13.sp,
                color = FormulooLabelGray,
            )
        }
    }
}

@Composable
fun PendingRequestItem(
    request: PendingRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(initials = request.initials, size = 40.dp, backgroundColor = FormulooPrimary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.employeeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = FormulooTextPrimary,
            )
            Text(
                text = "${request.type} · ${request.duration}",
                fontSize = 13.sp,
                color = FormulooLabelGray,
            )
        }
        IconButton(
            onClick = onApprove,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FormulooMint),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Approuver",
                tint = FormulooPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onReject,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FormulooErrorBg),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Refuser",
                tint = FormulooError,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun activityIconBackground(type: ActivityIconType): Color = when (type) {
    ActivityIconType.PAYROLL, ActivityIconType.OPPORTUNITY, ActivityIconType.EMPLOYEE -> FormulooMint
    ActivityIconType.INVOICE -> FormulooSecondaryBg
    ActivityIconType.DEFAULT -> FormulooBackground
}

private fun activityIconTint(type: ActivityIconType): Color = when (type) {
    ActivityIconType.PAYROLL, ActivityIconType.OPPORTUNITY, ActivityIconType.EMPLOYEE -> FormulooPrimary
    ActivityIconType.INVOICE -> FormulooSecondary
    ActivityIconType.DEFAULT -> FormulooOnSurfaceVariant
}

@Composable
private fun ActivityTypeIcon(type: ActivityIconType, tint: Color) {
    when (type) {
        ActivityIconType.PAYROLL -> Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = tint)
        ActivityIconType.OPPORTUNITY -> TargetIcon(tint = tint)
        ActivityIconType.INVOICE -> Icon(Icons.Filled.Receipt, contentDescription = null, tint = tint)
        ActivityIconType.EMPLOYEE -> Icon(Icons.Filled.Person, contentDescription = null, tint = tint)
        ActivityIconType.DEFAULT -> Icon(Icons.Filled.Notifications, contentDescription = null, tint = tint)
    }
}

@Composable
fun ActivityListItem(item: ActivityItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(activityIconBackground(item.iconType)),
            contentAlignment = Alignment.Center,
        ) {
            ActivityTypeIcon(type = item.iconType, tint = activityIconTint(item.iconType))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
            Text(item.subtitle, fontSize = 12.sp, color = FormulooLabelGray)
        }
        Spacer(Modifier.width(8.dp))
        Text(item.timeLabel, fontSize = 12.sp, color = FormulooLabelGray)
    }
}

private fun moduleIconBackground(type: ModuleIconType): Color = when (type) {
    ModuleIconType.HR -> FormulooPrimary
    ModuleIconType.ACCOUNTING -> FormulooSecondary
    ModuleIconType.CRM -> FormulooBlue
    ModuleIconType.STOCK -> FormulooPurple
    ModuleIconType.PROJECTS -> FormulooDeepOrange
    ModuleIconType.ANALYTICS -> FormulooGreen
    ModuleIconType.SETTINGS -> FormulooOnSurfaceVariant
    ModuleIconType.DASHBOARD -> FormulooPrimary
    ModuleIconType.DOCUMENTS -> FormulooTeal
}

@Composable
private fun ModuleTypeIcon(type: ModuleIconType, tint: Color) {
    when (type) {
        ModuleIconType.HR -> PeopleIcon(tint = tint)
        ModuleIconType.ACCOUNTING -> CalculateIcon(tint = tint)
        ModuleIconType.CRM -> TargetIcon(tint = tint)
        ModuleIconType.STOCK -> InventoryIcon(tint = tint)
        ModuleIconType.PROJECTS -> ProjectsIcon(tint = tint)
        ModuleIconType.ANALYTICS -> TrendingUpIcon(tint = tint)
        ModuleIconType.SETTINGS -> Icon(Icons.Filled.Settings, contentDescription = null, tint = tint)
        ModuleIconType.DASHBOARD -> DashboardIcon(tint = tint)
        ModuleIconType.DOCUMENTS -> Icon(Icons.Filled.Description, contentDescription = null, tint = tint)
    }
}

@Composable
fun ModuleCard(module: ModuleItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(moduleIconBackground(module.iconType)),
                contentAlignment = Alignment.Center,
            ) {
                ModuleTypeIcon(type = module.iconType, tint = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(module.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(module.subtitle, fontSize = 12.sp, color = FormulooLabelGray)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = FormulooTextPrimary,
        )
        if (actionLabel != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onAction),
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FormulooPrimary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FormulooPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

enum class BadgeTone { SUCCESS, WARNING, DANGER, NEUTRAL }

@Composable
fun StatusBadge(label: String, tone: BadgeTone, modifier: Modifier = Modifier, dot: Boolean = false) {
    val (backgroundColor, contentColor) = when (tone) {
        BadgeTone.SUCCESS -> FormulooMint to FormulooPrimary
        BadgeTone.WARNING -> FormulooSecondaryBg to FormulooSecondary
        BadgeTone.DANGER -> FormulooErrorBg to FormulooError
        BadgeTone.NEUTRAL -> FormulooOutline to FormulooOnSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(contentColor),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ModuleCheckCard(
    icon: @Composable (Color) -> Unit,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    helperText: String? = null,
) {
    val active = checked && enabled
    val borderColor = if (active) FormulooPrimary else FormulooOutline
    val backgroundColor = if (active) FormulooMint else FormulooSurface
    val rowAlpha = if (enabled) 1f else 0.5f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(BorderStroke(if (active) 1.5.dp else 1.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(rowAlpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) FormulooPrimary else FormulooBackground),
            contentAlignment = Alignment.Center,
        ) {
            icon(if (active) Color.White else FormulooOnSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooTextPrimary,
            )
            if (helperText != null) {
                Text(
                    text = helperText,
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = FormulooPrimary,
                uncheckedColor = FormulooOutline,
                checkmarkColor = Color.White,
            ),
        )
    }
}
