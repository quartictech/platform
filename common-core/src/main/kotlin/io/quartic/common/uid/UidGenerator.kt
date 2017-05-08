package io.quartic.common.uid

import java.util.function.Supplier

// TODO: eliminate this once we've removed all Java references, due to SAM vs. function weirdness
interface UidGenerator<T : Uid> : Supplier<T>
