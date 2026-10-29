/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune

import android.app.Application

// LeakCanary removido desta versão: ele gravava despejos de memória
// (arquivos de dezenas de MB) e mostrava notificações de relatório.
internal object LeakCanaryVariant {
  @JvmStatic
  fun initialize(@Suppress("UNUSED_PARAMETER") application: Application) = Unit

  @JvmStatic
  fun setEnabled(
    @Suppress("UNUSED_PARAMETER") context: android.content.Context,
    @Suppress("UNUSED_PARAMETER") enabled: Boolean,
  ) = Unit
}
