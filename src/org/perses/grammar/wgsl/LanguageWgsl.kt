package org.perses.grammar.wgsl

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.perses.program.EnumFormatControl
import org.perses.program.LanguageKind

object LanguageWgsl : LanguageKind(
  name = "wgsl",
  extensions = ImmutableSet.of("wgsl"),
  defaultCodeFormatControl = EnumFormatControl.COMPACT_ORIG_FORMAT,
  origCodeFormatControl = EnumFormatControl.ORIG_FORMAT,
  defaultFormatterCommands = ImmutableList.of(),
  allowedCodeFormatControl = ImmutableSet.of(
    EnumFormatControl.COMPACT_ORIG_FORMAT,
    EnumFormatControl.ORIG_FORMAT
  )
)
