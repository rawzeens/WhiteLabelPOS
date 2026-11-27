# White-label POS: how both apps behave

The Android app (Java) and the iPhone/iPad app (Swift) follow this one document, so a shop
can move between them with a backup file and get the same totals, receipts and reports.

Files in this folder:

| File | Used by | What it is |
|---|---|---|
| `build.xcconfig` | both builds | Reseller branding at build time: app name on the home screen, app id, version |
| `app/designs.json` | both apps at runtime | The 5 design environments |
| `app/catalogs.json` | both apps at runtime | Business types, starter products, demo shops |
| `app/currencies.json` | both apps at runtime | Currencies, symbols, decimals, cash rounding, usual tax name |
| `fixtures/money-cases.json` | Android tests, iPhone checks | Totals, money formatting and money typing cases |
| `fixtures/backup-sample.json` + `backup-checks.json` | Android tests, iPhone checks | A backup file both apps must read, and the report it must give |

## 1. Two levels of white-labelling

1. **Build level (reseller / agency).** Edit `build.xcconfig`: `APP_NAME` (name under the icon),
   `APP_ID` (Android application id and iOS bundle id), `APP_VERSION`, `APP_BUILD`. Swap the icon
   files. Build. Nothing in the code names a shop.
2. **Shop level (in the app).** First launch opens **Setup**. The shop owner enters the business
   name, tagline, logo, brand colour, accent colour, design environment, currency, tax and receipt
   footer. All of it can be changed later in **Settings**. Every screen and every receipt uses it.

## 2. Data file

The whole shop is one JSON document. It is saved on the device and it is also the backup file
(**Settings → Back up**). Unknown keys are ignored and missing keys get the defaults below, so a
backup from either app opens in the other.

```jsonc
{
  "format": "whitelabel-pos",     // always this
  "version": 1,
  "setupDone": true,              // false → app opens Setup
  "brand": {
    "name": "Kopi Pagi",          // required to finish setup
    "tagline": "",
    "logo": "",                   // base64 PNG, max 512×512, "" = show monogram
    "primary": "#6F4E37",         // brand colour
    "accent": "#E0A43B",          // second colour (badges, highlights)
    "design": "cafe"              // id from designs.json; unknown id → "fresh"
  },
  "business": {
    "type": "cafe",               // id from catalogs.json; default "cafe"
    "phone": "", "address": "", "regNo": "",
    "currency": "MYR",            // code from currencies.json; default "MYR"
    "taxName": "SST",
    "taxRate": 600,               // basis points: 600 = 6 %. 0 = no tax
    "taxInclusive": false,        // true = prices already include tax
    "cashRoundTo": 5,             // cash totals round to this step (smallest unit). 0 = off
    "footer": "Thank you!"        // last line of every receipt
  },
  "pin": "",                      // "" or 4 digits. Locks Settings, Products and refunds
  "categories": [ { "id": "c-1", "name": "Coffee" } ],
  "products": [ {
      "id": "p-1", "name": "Latte",
      "price": 1100,              // smallest unit
      "category": "c-1",          // "" = no category
      "icon": "☕",                // emoji, or "" = show initials
      "color": "#A47551",
      "sku": "",
      "trackStock": false, "stock": 0
  } ],
  "sales": [ {
      "id": "s-1001", "number": 1001,
      "time": 1763596800000,      // ms since 1970 (UTC)
      "lines": [ { "product": "p-1", "name": "Latte", "price": 1100, "qty": 2 } ],
      "discountType": "none",     // none | amount | percent
      "discountValue": 0,         // amount: smallest unit, percent: basis points
      "subtotal": 2200, "discount": 0, "tax": 132, "total": 2332,
      "rounding": -2, "due": 2330,
      "taxName": "SST", "taxRate": 600, "taxInclusive": false,  // copied at the time of sale
      "method": "cash",           // cash | card | qr | ewallet
      "paid": 5000, "change": 2670,
      "note": "",
      "refunded": false, "refundTime": 0
  } ],
  "nextNumber": 1002,             // next receipt number, starts at 1001
  "nextId": 100                   // counter for new ids: "p-<n>", "c-<n>"
}
```

Lines keep the name and price at the time of sale, so editing or deleting a product never changes
old receipts. Stock is lowered when a sale completes and put back when it is refunded (only for
products with `trackStock`). A product with `trackStock` and `stock <= 0` shows **Sold out** and
can't be added.

## 3. Money rules

All amounts are whole numbers in the smallest unit. `roundDiv(a, b)` is round-half-up of `a / b`
for `a >= 0`: `(2a + b) / (2b)` in integer maths.

```
subtotal = Σ price × qty
discount = amount  → clamp(value, 0, subtotal)
           percent → min(subtotal, roundDiv(subtotal × clamp(value, 0, 10000), 10000))
           none    → 0
net      = subtotal − discount
tax      = rate = 0    → 0,                                     total = net
           inclusive   → net − roundDiv(net × 10000, 10000 + rate), total = net
           exclusive   → roundDiv(net × rate, 10000),            total = net + tax
rounding = method is cash and step > 0 → ((total + step / 2) / step) × step − total   (integer division)
           otherwise → 0
due      = total + rounding
paid     = cash → what the customer handed over (must be ≥ due); other methods → due
change   = max(0, paid − due)
```

**Showing money:** `symbol` + (a space if the symbol ends in a letter) + digits with `,` every
three places + `.` + `decimals` digits. Negative amounts start with `-`: `-RM 2.50`, `$1,000.00`,
`Rp 1,234,567`, `€0.99`.

**Typing money:** remove everything except digits and `.` (so `RM 1,234.56` → `1234.56`). Empty,
or more than one `.`, is invalid. Extra decimals round half up (`12.345` → 1235 with 2 decimals).

**Starter prices** in `catalogs.json` are written for 2 decimals. With a 0-decimal currency multiply
them by 100.

**Cash quick buttons** on the checkout: *Exact*, then the next whole note amounts above the due,
taken from the steps 1, 5, 10, 20, 50, 100 × `unit` (`unit` = 10^decimals, or 1000 when decimals = 0):
for each step the smallest multiple ≥ due, skipping duplicates and the exact amount; show up to 4.

## 4. Reports

Reports cover `[from, to)` and use the stored sale values (never recompute old sales).

- sales in range with `refunded = false` count; refunded ones count only as refunds.
- `gross` = Σ due, `sales` = count, `average` = roundDiv(gross, sales) (0 when none),
  `tax` = Σ tax, `discount` = Σ discount, `refunds` = count refunded, `refunded` = Σ due of refunded.
- `byMethod` = Σ due per method (only methods that appear).
- `top` = products by name: qty = Σ qty, amount = Σ price × qty; sort by qty desc, then amount desc,
  then name. Show 5.
- Chart: **Today** → one bar per hour 6:00–23:00 (anything before 6:00 goes in the 6:00 bar); **7 days / 30 days** → one bar per local day.

## 5. The 5 design environments

`designs.json` tokens: `page` (screen background), `surface` (cards), `raised` (fields, chips),
`ink` (main text), `muted` (secondary text), `line` (borders), `dark`, `radius` (corner radius in
points for cards/tiles), `font`, `caps` (uppercase headings, buttons, labels and product names),
`tracking` (letter spacing in em for those), `header`, `products`, `columns` (phone portrait),
`buttons`, `logoShape`, `chips`.

Brand colour helpers (same in both apps):
- `on(color)`: text colour on top of a colour. Relative luminance L (sRGB, linearised). Use white
  if `1.05 / (L + 0.05) >= (L + 0.05) / 0.05`, else `#111111`.
- `soft(color)`: the colour at 14 % over `page` (22 % when `dark`).

Fonts: `rounded` → Android `sans-serif` medium / iOS `.rounded`; `sans` → system sans;
`serif` → Android `serif` / iOS `.serif`; `condensed` → Android `sans-serif-condensed` bold /
iOS system with `.width(.condensed)` heavy; `light` → Android `sans-serif-light` / iOS system light.

| | Fresh | Midnight | Café | Street | Atelier |
|---|---|---|---|---|---|
| Mood | bright, clean, rounded | dark with glow | warm paper menu | bold blocks | quiet minimal |
| Header (`header`) | `plain`: logo 44 + name bold + tagline, left | `glow`: like plain on dark, thin brand-colour line under it | `centered`: logo 64 on top, name, tagline in italic, all centred | `bar`: full-width brand-colour bar, logo + name uppercase in `on(primary)` | `minimal`: small logo + name uppercase spaced, centred, hairline under |
| Products (`products`) | `tiles`: white rounded cards, emoji in a circle of `soft(product color)`, name 2 lines, price bold in primary | `glow`: dark cards, 4-pt product-colour stripe on the left, emoji + name, price large in primary; a card in the cart gets a primary border | `menu`: list grouped by category with serif category titles, rows `name ……… price` with a dotted leader, emoji small at the left | `blocks`: square blocks filled with the product colour (primary if none), name uppercase heavy in `on(color)`, price at the bottom, big faint emoji top-right | `lines`: rows split by hairlines, name uppercase spaced, SKU or category muted underneath, price right; no emoji |
| Buttons | rounded (radius) | rounded | pill | square, heavy, uppercase | square, thin, uppercase spaced |
| Logo/monogram (`logoShape`) | circle | rounded square | circle with a primary ring | square | square outline (monogram in ink, no fill) |
| Category chips (`chips`) | `soft`: raised fill, selected = primary fill | `outline`: 1-pt line, selected = primary line + primary text | `pill`: raised pill, selected = primary | `solid`: ink fill + white text, selected = primary | `underline`: text only, selected = ink text with a 2-pt primary underline |

On every design: primary colour fills the main action (Charge / Complete sale / Next), the
accent fills the cart badge and "in cart" quantity badges. Quantity badges on products show how many
of that product are already in the cart. Tablets / wide screens show more columns
(about one column per 150 pt; `menu`/`lines` stay 1 column but in 2 side-by-side lists over 900 pt)
and keep the cart open on the right.

The monogram is the brand initials: the words of the name whose first character is a letter or
digit; with 2+ words take the first character of the first and the last word, with 1 word its
first character, with none `?`; uppercase. Products with an empty icon use the same rule.

## 6. Setup (first launch)

A progress line on top shows steps 1–6. **Back** and **Next** at the bottom. Every step shows a
live preview in the chosen design with the chosen brand.

0. **Welcome**: "Your shop. Your brand. Your till." Buttons: *Set up my shop*, *Restore a backup*,
   *Try a demo shop* (pick one of the 5 business types → fills everything with its demo shop).
1. **Business**: name (required), tagline, kind of business (5 cards from `catalogs.json`), phone,
   address, registration number.
2. **Logo & colours**: logo (*Choose a picture* / *Remove*; scaled down to 512 px, PNG); brand
   colour (14 swatches + hex field); accent colour (8 swatches + hex). Preview: header + a few
   products.
3. **Design**: the 5 environments as cards, each a small live preview (header + 4 products + Charge
   button) in the shop's own name and colours. Tap to choose.
4. **Money & receipt**: currency (list), tax name, tax rate %, *Prices already include tax*,
   *Round cash to the nearest …* (only if the currency's `cashRoundTo` > 0, or the shop's value
   is already > 0), receipt footer. Preview: a small receipt.
5. **Finish**: *Add the starter products for <type>* (on by default) or start empty; owner PIN
   (optional, 4 digits, typed twice); *Open my POS*.

Choosing a currency sets the tax name to the currency's `taxName` and `cashRoundTo` to its default
(the owner can change both afterwards). Defaults on a new setup: MYR, SST, 0 %, exclusive, round 5.

## 7. Screens

Tabs: **Sell · Orders · Products · Reports · Settings**. Settings and Products ask for the PIN when
one is set (once per app launch). Refunds always ask when a PIN is set.

- **Sell**: design header, search box, category chips (*All* first), products in the design's
  layout. Tap adds one. Phones show a bottom bar `<n> items · <due> · Charge`; tapping it opens the
  **Cart**. Wide screens show the cart on the right all the time.
- **Cart**: lines with − / + (− at 1 removes the line), *Discount* (amount or %), *Note*,
  *Clear*, totals (Subtotal, Discount, Tax `<name> <rate>%` or `Includes <name> <rate>%`, Total),
  big **Charge <total>** button.
- **Checkout**: due shown big; method buttons *Cash · Card · QR pay · E-wallet*. Cash: amount
  received field + quick buttons + change (rounding line shown when not 0). Others: *Mark as paid*
  (the app does not move money, it records how the customer paid). **Complete sale** saves it,
  lowers stock, clears the cart and opens the receipt.
- **Receipt**: logo/monogram, name, tagline, address, phone, registration no, `Receipt #1001`, date
  time, lines (`2 × Latte    RM 22.00`), Subtotal, Discount, Tax, Rounding, Total, Paid by, Cash,
  Change, note, footer. Receipt uses the design's font but is always dark text on white (it looks
  like paper). Buttons: *Share receipt* (as a picture), *New sale*.
- **Orders**: grouped by day (Today, Yesterday, date), rows `#1001 · 10:24 · 3 items · Cash ·
  RM 32.85`, refunded rows say **Refunded**. Search by receipt number. Tap → receipt with
  *Share* and *Refund*.
- **Products**: search, grouped by category, row shows icon, name, price, stock (`12 left`,
  `Sold out`). *Add product*, *Categories* (add, rename, delete → products move to no category).
  Edit: name, price, category, icon (emoji or blank), colour (swatches), SKU, *Track stock* +
  amount in stock, *Delete*.
- **Reports**: *Today · 7 days · 30 days*; cards Sales, Orders, Average sale, Tax; bar chart;
  by payment method; top products; refunds line.
- **Settings**: brand card (preview of the header), *Business details*, *Logo & colours*,
  *Design*, *Money & receipt* (each opens that setup step on its own with **Save**), *Owner PIN*,
  *Back up* (share the JSON file), *Restore* (pick a JSON file, confirm, replace), *Try a demo shop*,
  *Start over* (erase and open setup), about line `<APP_NAME> <version>`.

All words on screen are plain shop language: no file names, codes or technical terms.

## 8. Demo shop and screenshot switches

*Try a demo shop* for type T: brand and business from T's `demo`, MYR, tax SST 6 % exclusive,
cash round 5, the starter products, 30 days of made-up sales, `setupDone = true`.

Made-up sales (identical on both apps): `state = 42`, `next() = state = (state × 1103515245 + 12345)
mod 2^31`, `rand(n) = next() mod n`. For `d` from 29 down to 0 (day = local midnight today − d days):
`count = 5 + rand(8)`; for each sale: `minute = 480 + rand(780)`; `lineCount = 1 + rand(3)`; for each
line `index = rand(products)`, `qty = 1 + rand(2)` (same product twice → add the qty);
`m = rand(10)` (0–3 cash, 4–6 card, 7–8 qr, 9 ewallet); `rand(12) == 0` → 10 % discount;
`rand(40) == 0` → refunded (refundTime = time + 30 min). Cash pays the next multiple of 1000 that
is ≥ due. Skip sales whose time is after now (still draw all their numbers). Sort by time, number
from 1001, id `s-<number>`. Stock is not changed by made-up sales.

Debug builds accept switches so screenshots can be taken without tapping:

| | Android (`adb shell am start -S -n <APP_ID>/com.r4xn.whitelabelpos.ui.MainActivity …`) | iPhone (launch arguments) |
|---|---|---|
| Load demo type | `--es demo grocery` | `-demo grocery` |
| Use another design | `--es design midnight` | `-design midnight` |
| Open a screen | `--es open sell\|till\|cart\|checkout\|receipt\|orders\|products\|product\|reports\|settings\|setup0..setup5` | `-screen …` same names |

`till` is the Sell screen with the sample cart (shows the in-cart badges, and the cart panel on tablets).
With `till`, `cart`, `checkout` and `receipt` the cart gets products 0 × 2, 4 × 1, 9 × 1 (those that exist);
`receipt` completes that cart as a cash sale paying the next multiple of 1000 and shows it.
`product` opens the edit screen for the first product. `setupN` opens setup step N filled with the
demo data.

## 9. Details both apps settled while being built

- Typing money: only ASCII digits count; more than 13 whole digits is invalid; with 0 decimals a
  fraction rounds half up (`7.6` → 8).
- Colour mixing rounds each channel to the nearest whole number (`soft(#FF0000)` on white = `#FFDBDB`).
  Luminance uses the 0.03928 cut-off. By this rule the Glow Studio pink `#E0457B` gets dark text.
- Made-up sales with the same time keep the order they were made in.
- Refunding a sale twice does nothing. Finishing setup unlocks the PIN for that launch.
- Restoring a backup from the Welcome screen opens the till straight away.
