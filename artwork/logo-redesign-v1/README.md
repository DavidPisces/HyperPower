# HyperPower Logo Redesign — Round 1

This exploration follows the installed `logo-generator` workflow. It keeps the product's existing cyan/lime-on-charcoal identity while replacing the conventional chip + ECG combination with more ownable forms.

## Brand brief inferred from the product

- Product: Android device power and CPU telemetry tool
- Core concepts: real-time monitoring, performance, energy, precision
- Mood: technical, focused, premium, dark
- Required behavior: legible as a 16 px notification mark, a 48 dp launcher glyph, and a monochrome themed icon
- Palette: `#071012`, `#53D7F4`, `#B8F25A`, `#F0F6F4`

## Directions

1. **Hyperframe** — A bold H monogram with a rising/falling power bridge. The clearest app-icon candidate and the strongest connection to the name.
2. **H Array** — Modular telemetry cells form an H; the detached boosted capsule suggests a performance core breaking its limit.
3. **Live Trace** — A single monoline H contains the live signal. Familiar enough to communicate instantly, but substantially simpler than the current chip outline.
4. **Core Aperture** — A dense circle with an H removed from it. The negative space makes the monitoring “core” feel precise and premium.
5. **Torque Ring** — A performance gauge and energy bolt. Best when immediate “power/performance” recognition matters more than the H monogram.
6. **Core Topology** — Four compute nodes converge on a central monitor core. Best reflects CPU clusters, data access, and multi-metric telemetry.

All SVG source files use `viewBox="0 0 100 100"` and `currentColor`, so one geometry supports brand color, monochrome, themed icons, light mode, and dark mode.

Open `showcase.html` to compare palette, background, and size behavior. `contact-sheet.svg` and the rendered PNG provide a quick selection board. No production Android resources are replaced in this round; the selected direction will still receive an Android adaptive-icon safe-zone pass.
