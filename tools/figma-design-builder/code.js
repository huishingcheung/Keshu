figma.showUI(__html__, { width: 410, height: 620, themeColors: true });

const NS = "jnu.smartedu.designbuilder";
const COLLECTION_NAME = "JNU Smart Edu / Dark";
const ROOT_KEYS = {
  foundations: "foundations-root",
  components: "components-root",
  screens: "screens-root",
  widgets: "widgets-root",
};

const COLORS = {
  "color/background": ["#0B0E14", ["FRAME_FILL", "SHAPE_FILL"], "MaterialTheme.colorScheme.background"],
  "color/surface": ["#121721", ["FRAME_FILL", "SHAPE_FILL"], "MaterialTheme.colorScheme.surface"],
  "color/surface-variant": ["#202633", ["FRAME_FILL", "SHAPE_FILL"], "MaterialTheme.colorScheme.surfaceVariant"],
  "color/primary": ["#B7C7FF", ["FRAME_FILL", "SHAPE_FILL", "TEXT_FILL", "STROKE_COLOR"], "MaterialTheme.colorScheme.primary"],
  "color/on-primary": ["#152B73", ["FRAME_FILL", "SHAPE_FILL", "TEXT_FILL"], "MaterialTheme.colorScheme.onPrimary"],
  "color/primary-container": ["#2C438C", ["FRAME_FILL", "SHAPE_FILL", "STROKE_COLOR"], "MaterialTheme.colorScheme.primaryContainer"],
  "color/on-primary-container": ["#DDE4FF", ["TEXT_FILL", "SHAPE_FILL"], "MaterialTheme.colorScheme.onPrimaryContainer"],
  "color/secondary": ["#72DEC2", ["FRAME_FILL", "SHAPE_FILL", "TEXT_FILL"], "MaterialTheme.colorScheme.secondary"],
  "color/secondary-container": ["#005143", ["FRAME_FILL", "SHAPE_FILL"], "MaterialTheme.colorScheme.secondaryContainer"],
  "color/tertiary": ["#E0B6FF", ["FRAME_FILL", "SHAPE_FILL", "TEXT_FILL"], "MaterialTheme.colorScheme.tertiary"],
  "color/text-primary": ["#E5E7F0", ["TEXT_FILL", "SHAPE_FILL"], "MaterialTheme.colorScheme.onSurface"],
  "color/text-secondary": ["#C2C6D4", ["TEXT_FILL", "SHAPE_FILL"], "MaterialTheme.colorScheme.onSurfaceVariant"],
  "color/outline": ["#454B59", ["STROKE_COLOR"], "MaterialTheme.colorScheme.outline"],
  "color/error": ["#FFB4AB", ["TEXT_FILL", "SHAPE_FILL", "STROKE_COLOR"], "MaterialTheme.colorScheme.error"],
  "color/warning": ["#FDBA74", ["TEXT_FILL", "SHAPE_FILL", "STROKE_COLOR"], "Widget.warning"],
};

const SPACING = {
  "spacing/xs": 4,
  "spacing/sm": 8,
  "spacing/md": 12,
  "spacing/lg": 16,
  "spacing/xl": 20,
  "spacing/2xl": 24,
  "spacing/3xl": 32,
};

const RADII = {
  "radius/xs": 8,
  "radius/sm": 12,
  "radius/md": 18,
  "radius/lg": 24,
  "radius/xl": 32,
  "radius/full": 999,
};

const TYPE_SPECS = {
  "JNU/Headline/Large": [30, 36, "Bold"],
  "JNU/Headline/Small": [23, 29, "Bold"],
  "JNU/Title/Large": [20, 26, "Bold"],
  "JNU/Title/Medium": [16, 22, "Medium"],
  "JNU/Title/Small": [14, 20, "Medium"],
  "JNU/Body/Large": [16, 24, "Regular"],
  "JNU/Body/Medium": [14, 21, "Regular"],
  "JNU/Body/Small": [12, 18, "Regular"],
  "JNU/Label/Medium": [12, 16, "Medium"],
  "JNU/Label/Small": [11, 15, "Medium"],
};

let fonts = null;
let tokenMap = {};
let textStyles = {};

figma.ui.onmessage = async message => {
  if (message.type !== "run") return;
  try {
    await prepareRuntime();
    let result;
    if (message.command === "foundations") result = await buildFoundations();
    else if (message.command === "components") result = await buildComponents();
    else if (message.command === "screens") result = await buildScreens();
    else if (message.command === "widgets") result = await buildWidgetsAndQa();
    else throw new Error(`未知阶段：${message.command}`);
    figma.ui.postMessage({ ok: true, title: result.title, summary: result.summary });
    if (result.focus && result.focus.length) figma.viewport.scrollAndZoomIntoView(result.focus);
  } catch (error) {
    figma.ui.postMessage({ ok: false, error: error instanceof Error ? error.message : String(error) });
  }
};

async function prepareRuntime() {
  if (!fonts) fonts = await resolveFonts();
  await Promise.all(Object.values(fonts).map(font => figma.loadFontAsync(font)));
  await refreshResources();
}

async function resolveFonts() {
  const available = await figma.listAvailableFontsAsync();
  const has = (family, style) => available.some(item => item.fontName.family === family && item.fontName.style === style);
  const family = has("Roboto", "Regular") ? "Roboto" : "Inter";
  const pick = candidates => candidates.find(style => has(family, style)) || "Regular";
  return {
    regular: { family, style: pick(["Regular"]) },
    medium: { family, style: pick(["Medium", "Semi Bold", "Regular"]) },
    bold: { family, style: pick(["Bold", "Semi Bold", "Medium"]) },
  };
}

async function refreshResources() {
  const variables = await figma.variables.getLocalVariablesAsync();
  tokenMap = Object.fromEntries(variables.map(variable => [variable.name, variable]));
  const styles = await figma.getLocalTextStylesAsync();
  textStyles = Object.fromEntries(styles.map(style => [style.name, style]));
}

function hex(hexValue) {
  const value = hexValue.replace("#", "");
  return {
    r: parseInt(value.slice(0, 2), 16) / 255,
    g: parseInt(value.slice(2, 4), 16) / 255,
    b: parseInt(value.slice(4, 6), 16) / 255,
  };
}

function paint(hexValue, opacity = 1) {
  return { type: "SOLID", color: hex(hexValue), opacity };
}

function setFill(node, tokenName, fallback, opacity = 1) {
  let value = paint(fallback, opacity);
  const variable = tokenMap[tokenName];
  if (variable) value = figma.variables.setBoundVariableForPaint(value, "color", variable);
  node.fills = [value];
}

function setStroke(node, tokenName, fallback, opacity = 1) {
  let value = paint(fallback, opacity);
  const variable = tokenMap[tokenName];
  if (variable) value = figma.variables.setBoundVariableForPaint(value, "color", variable);
  node.strokes = [value];
  node.strokeWeight = 1;
}

function bindNumber(node, property, tokenName, fallback) {
  node[property] = fallback;
  const variable = tokenMap[tokenName];
  if (variable) node.setBoundVariable(property, variable);
}

function tag(node, key, phase) {
  node.setSharedPluginData(NS, "key", key);
  node.setSharedPluginData(NS, "phase", phase);
  node.setSharedPluginData(NS, "version", "2");
  return node;
}

function findTagged(key) {
  return figma.currentPage.findOne(node => node.getSharedPluginData && node.getSharedPluginData(NS, "key") === key);
}

function taggedRoots() {
  return figma.currentPage.children.filter(node => node.getSharedPluginData && node.getSharedPluginData(NS, "version") === "2");
}

function getBaseX() {
  const existing = taggedRoots();
  if (existing.length) return Math.min(...existing.map(node => node.x));
  return figma.currentPage.children.reduce((max, node) => Math.max(max, node.x + node.width), 0) + 240;
}

function rebuildRoot(key, name, phase, width, y) {
  const previous = findTagged(key);
  const x = previous ? previous.x : getBaseX();
  const previousY = previous ? previous.y : y;
  if (previous) previous.remove();
  const root = verticalFrame(name, width, 32, 48);
  root.x = x;
  root.y = previousY;
  root.clipsContent = false;
  setFill(root, "color/background", "#0B0E14");
  tag(root, key, phase);
  return root;
}

function verticalFrame(name, width, gap = 0, padding = 0) {
  const frame = figma.createFrame();
  frame.name = name;
  frame.layoutMode = "VERTICAL";
  frame.primaryAxisSizingMode = "AUTO";
  frame.counterAxisSizingMode = "FIXED";
  frame.resize(width, 100);
  frame.itemSpacing = gap;
  frame.paddingTop = padding;
  frame.paddingRight = padding;
  frame.paddingBottom = padding;
  frame.paddingLeft = padding;
  frame.fills = [];
  frame.clipsContent = false;
  return frame;
}

function horizontalFrame(name, gap = 0, padding = 0, width = null) {
  const frame = figma.createFrame();
  frame.name = name;
  frame.layoutMode = "HORIZONTAL";
  frame.primaryAxisSizingMode = width == null ? "AUTO" : "FIXED";
  frame.counterAxisSizingMode = "AUTO";
  frame.itemSpacing = gap;
  frame.paddingTop = padding;
  frame.paddingRight = padding;
  frame.paddingBottom = padding;
  frame.paddingLeft = padding;
  frame.fills = [];
  frame.clipsContent = false;
  if (width != null) frame.resize(width, 100);
  return frame;
}

function fixedFrame(name, width, height) {
  const frame = figma.createFrame();
  frame.name = name;
  frame.resize(width, height);
  frame.clipsContent = true;
  return frame;
}

function stretch(parent, child) {
  parent.appendChild(child);
  if (parent.layoutMode === "VERTICAL") child.layoutAlign = "STRETCH";
  return child;
}

function fontFor(styleName) {
  if (styleName.includes("Headline") || styleName.includes("Title/Large")) return fonts.bold;
  if (styleName.includes("Title") || styleName.includes("Label")) return fonts.medium;
  return fonts.regular;
}

function makeText(content, styleName = "JNU/Body/Medium", colorToken = "color/text-primary", width = null, name = "Text") {
  const node = figma.createText();
  node.name = name;
  node.fontName = fontFor(styleName);
  node.characters = content;
  const style = textStyles[styleName];
  if (style) node.textStyleId = style.id;
  if (width != null) {
    node.resize(width, 20);
    node.textAutoResize = "HEIGHT";
  } else {
    node.textAutoResize = "WIDTH_AND_HEIGHT";
  }
  setFill(node, colorToken, colorToken === "color/text-secondary" ? "#C2C6D4" : "#E5E7F0");
  return node;
}

function titleBlock(eyebrow, title, body, width) {
  const block = verticalFrame("Title Block", width, 8, 0);
  stretch(block, makeText(eyebrow.toUpperCase(), "JNU/Label/Medium", "color/primary", width));
  stretch(block, makeText(title, "JNU/Headline/Large", "color/text-primary", width));
  stretch(block, makeText(body, "JNU/Body/Medium", "color/text-secondary", width));
  return block;
}

function card(name, width, gap = 12, padding = 16, fillToken = "color/surface") {
  const frame = verticalFrame(name, width, gap, padding);
  setFill(frame, fillToken, fillToken === "color/surface-variant" ? "#202633" : "#121721");
  bindNumber(frame, "cornerRadius", "radius/md", 18);
  setStroke(frame, "color/outline", "#454B59", 0.35);
  return frame;
}

async function ensureVariables() {
  const collections = await figma.variables.getLocalVariableCollectionsAsync();
  let collection = collections.find(item => item.name === COLLECTION_NAME);
  if (!collection) {
    collection = figma.variables.createVariableCollection(COLLECTION_NAME);
    collection.renameMode(collection.modes[0].modeId, "Dark");
  }
  const modeId = collection.modes[0].modeId;
  const current = await figma.variables.getLocalVariablesAsync();
  const byName = Object.fromEntries(current.filter(item => item.variableCollectionId === collection.id).map(item => [item.name, item]));

  for (const [name, [value, scopes, syntax]] of Object.entries(COLORS)) {
    const variable = byName[name] || figma.variables.createVariable(name, collection, "COLOR");
    variable.setValueForMode(modeId, hex(value));
    variable.scopes = scopes;
    variable.setVariableCodeSyntax("ANDROID", syntax);
  }
  for (const [name, value] of Object.entries(SPACING)) {
    const variable = byName[name] || figma.variables.createVariable(name, collection, "FLOAT");
    variable.setValueForMode(modeId, value);
    variable.scopes = ["GAP"];
    variable.setVariableCodeSyntax("ANDROID", `${value}.dp`);
  }
  for (const [name, value] of Object.entries(RADII)) {
    const variable = byName[name] || figma.variables.createVariable(name, collection, "FLOAT");
    variable.setValueForMode(modeId, value);
    variable.scopes = ["CORNER_RADIUS"];
    variable.setVariableCodeSyntax("ANDROID", value === 999 ? "CircleShape" : `RoundedCornerShape(${value}.dp)`);
  }
  await refreshResources();
}

async function ensureStyles() {
  const existing = await figma.getLocalTextStylesAsync();
  for (const [name, [size, lineHeight, weight]] of Object.entries(TYPE_SPECS)) {
    let style = existing.find(item => item.name === name);
    if (!style) style = figma.createTextStyle();
    style.name = name;
    style.fontName = weight === "Bold" ? fonts.bold : weight === "Medium" ? fonts.medium : fonts.regular;
    style.fontSize = size;
    style.lineHeight = { unit: "PIXELS", value: lineHeight };
    style.description = `Compose typography · ${size}sp / ${lineHeight}sp`;
  }
  const effects = await figma.getLocalEffectStylesAsync();
  const shadowDefinitions = [
    ["JNU/Elevation/Subtle", 2, 0.16],
    ["JNU/Elevation/Floating", 12, 0.24],
  ];
  for (const [name, radius, alpha] of shadowDefinitions) {
    let style = effects.find(item => item.name === name);
    if (!style) style = figma.createEffectStyle();
    style.name = name;
    style.effects = [{
      type: "DROP_SHADOW",
      color: { r: 0, g: 0, b: 0, a: alpha },
      offset: { x: 0, y: radius === 2 ? 1 : 6 },
      radius,
      spread: 0,
      visible: true,
      blendMode: "NORMAL",
    }];
  }
  await refreshResources();
}

async function buildFoundations() {
  await ensureVariables();
  await ensureStyles();
  const root = rebuildRoot(ROOT_KEYS.foundations, "JNU • Foundations", "phase1", 1540, 0);
  stretch(root, titleBlock("JNU Smart Edu", "Dark Campus Productivity System", "Material 3 foundations mapped directly from the Android Compose implementation. Starter-compatible: one dark variable mode.", 1444));

  const colorSection = verticalFrame("Color Tokens", 1444, 16, 0);
  stretch(colorSection, makeText("Semantic color", "JNU/Headline/Small", "color/text-primary", 1444));
  const swatches = horizontalFrame("Color Swatches", 12, 0, 1444);
  swatches.layoutWrap = "WRAP";
  swatches.counterAxisSpacing = 12;
  for (const [name, [value]] of Object.entries(COLORS)) {
    const item = verticalFrame(name, 164, 8, 10);
    setFill(item, "color/surface", "#121721");
    bindNumber(item, "cornerRadius", "radius/sm", 12);
    const sample = fixedFrame("Sample", 144, 72);
    setFill(sample, name, value);
    bindNumber(sample, "cornerRadius", "radius/xs", 8);
    item.appendChild(sample);
    stretch(item, makeText(name.replace("color/", ""), "JNU/Label/Medium", "color/text-primary", 144));
    stretch(item, makeText(value, "JNU/Label/Small", "color/text-secondary", 144));
    swatches.appendChild(item);
  }
  stretch(colorSection, swatches);
  stretch(root, colorSection);

  const foundationRow = horizontalFrame("Type and Geometry", 24, 0, 1444);
  const typeCard = card("Typography", 900, 14, 20);
  for (const name of Object.keys(TYPE_SPECS)) stretch(typeCard, makeText(`${name.replace("JNU/", "")}  暨南大学智慧教务`, name, "color/text-primary", 860));
  foundationRow.appendChild(typeCard);
  const geometry = card("Spacing and Radius", 520, 16, 20);
  stretch(geometry, makeText("Spacing", "JNU/Title/Large", "color/text-primary", 480));
  for (const [name, value] of Object.entries(SPACING)) {
    const row = horizontalFrame(name, 12, 0);
    const bar = fixedFrame("Bar", value * 4, 12);
    setFill(bar, "color/secondary", "#72DEC2");
    bindNumber(bar, "cornerRadius", "radius/full", 999);
    row.appendChild(bar);
    row.appendChild(makeText(`${name} · ${value}dp`, "JNU/Label/Small", "color/text-secondary"));
    geometry.appendChild(row);
  }
  stretch(geometry, makeText("Radius", "JNU/Title/Large", "color/text-primary", 480));
  const radiusRow = horizontalFrame("Radius Samples", 10, 0);
  for (const [name, value] of Object.entries(RADII)) {
    const sample = fixedFrame(name, 64, 48);
    setFill(sample, "color/primary-container", "#2C438C");
    sample.cornerRadius = Math.min(value, 24);
    if (tokenMap[name]) sample.setBoundVariable("cornerRadius", tokenMap[name]);
    radiusRow.appendChild(sample);
  }
  geometry.appendChild(radiusRow);
  foundationRow.appendChild(geometry);
  stretch(root, foundationRow);

  return {
    title: "Phase 1 · Foundations",
    summary: `${Object.keys(COLORS).length + Object.keys(SPACING).length + Object.keys(RADII).length} 个变量 · ${Object.keys(TYPE_SPECS).length} 个文字样式 · 2 个阴影样式`,
    focus: [root],
  };
}

function componentText(component, name, value, style, colorToken, width = null) {
  const node = makeText(value, style, colorToken, width, name);
  component.appendChild(node);
  return node;
}

function createButtonVariant(type, state) {
  const component = figma.createComponent();
  component.name = `Type=${type}, State=${state}`;
  component.layoutMode = "HORIZONTAL";
  component.primaryAxisSizingMode = "FIXED";
  component.counterAxisSizingMode = "FIXED";
  component.primaryAxisAlignItems = "CENTER";
  component.counterAxisAlignItems = "CENTER";
  component.resize(132, 44);
  bindNumber(component, "cornerRadius", "radius/sm", 12);
  const disabled = state === "Disabled";
  setFill(component, type === "Primary" ? "color/primary" : "color/surface-variant", type === "Primary" ? "#B7C7FF" : "#202633", disabled ? 0.42 : 1);
  if (type !== "Primary") setStroke(component, "color/outline", "#454B59", disabled ? 0.3 : 0.8);
  const label = componentText(component, "Label", "操作按钮", "JNU/Label/Medium", type === "Primary" ? "color/on-primary" : "color/text-primary");
  label.opacity = disabled ? 0.55 : 1;
  const property = component.addComponentProperty("Label", "TEXT", "操作按钮");
  label.componentPropertyReferences = { characters: property };
  return component;
}

function createNavVariant(selected) {
  const component = figma.createComponent();
  component.name = `Selected=${selected ? "True" : "False"}`;
  component.layoutMode = "VERTICAL";
  component.primaryAxisSizingMode = "FIXED";
  component.counterAxisSizingMode = "FIXED";
  component.primaryAxisAlignItems = "CENTER";
  component.counterAxisAlignItems = "CENTER";
  component.itemSpacing = 3;
  component.resize(58, 54);
  component.fills = [];
  const icon = fixedFrame("Icon Container", 32, 28);
  setFill(icon, selected ? "color/primary" : "color/surface-variant", selected ? "#B7C7FF" : "#202633", selected ? 1 : 0);
  bindNumber(icon, "cornerRadius", "radius/full", 999);
  const glyph = makeText("●", "JNU/Label/Small", selected ? "color/on-primary" : "color/text-secondary");
  glyph.x = 11;
  glyph.y = 6;
  icon.appendChild(glyph);
  component.appendChild(icon);
  const label = componentText(component, "Label", "首页", "JNU/Label/Small", selected ? "color/primary" : "color/text-secondary");
  const property = component.addComponentProperty("Label", "TEXT", "首页");
  label.componentPropertyReferences = { characters: property };
  return component;
}

function createTaskVariant(done) {
  const component = figma.createComponent();
  component.name = `Done=${done ? "True" : "False"}`;
  component.layoutMode = "HORIZONTAL";
  component.primaryAxisSizingMode = "FIXED";
  component.counterAxisSizingMode = "FIXED";
  component.counterAxisAlignItems = "CENTER";
  component.itemSpacing = 10;
  component.paddingLeft = 12;
  component.paddingRight = 12;
  component.resize(320, 64);
  setFill(component, done ? "color/surface-variant" : "color/surface", done ? "#202633" : "#121721", done ? 0.7 : 1);
  bindNumber(component, "cornerRadius", "radius/md", 18);
  const check = fixedFrame("Check", 24, 24);
  check.fills = [];
  setStroke(check, done ? "color/secondary" : "color/outline", done ? "#72DEC2" : "#454B59");
  bindNumber(check, "cornerRadius", "radius/full", 999);
  if (done) {
    const mark = makeText("✓", "JNU/Label/Medium", "color/secondary");
    mark.x = 6;
    mark.y = 3;
    check.appendChild(mark);
  }
  component.appendChild(check);
  const copy = verticalFrame("Copy", 240, 1, 0);
  const title = componentText(copy, "Title", "完成课程作业", "JNU/Title/Small", done ? "color/text-secondary" : "color/text-primary", 240);
  const due = componentText(copy, "Meta", "7月3日截止 · 点击编辑", "JNU/Body/Small", "color/text-secondary", 240);
  component.appendChild(copy);
  const titleProperty = component.addComponentProperty("Title", "TEXT", "完成课程作业");
  const metaProperty = component.addComponentProperty("Meta", "TEXT", "7月3日截止 · 点击编辑");
  title.componentPropertyReferences = { characters: titleProperty };
  due.componentPropertyReferences = { characters: metaProperty };
  return component;
}

function createCourseVariant(tone) {
  const component = figma.createComponent();
  component.name = `Tone=${tone}`;
  component.layoutMode = "VERTICAL";
  component.primaryAxisSizingMode = "FIXED";
  component.counterAxisSizingMode = "FIXED";
  component.itemSpacing = 3;
  component.paddingTop = 8;
  component.paddingRight = 4;
  component.paddingBottom = 6;
  component.paddingLeft = 4;
  component.resize(40, 112);
  const fillToken = tone === "Mint" ? "color/secondary-container" : tone === "Purple" ? "color/surface-variant" : "color/primary-container";
  setFill(component, fillToken, tone === "Mint" ? "#005143" : tone === "Purple" ? "#3A2F4E" : "#2C438C");
  bindNumber(component, "cornerRadius", "radius/xs", 8);
  const title = componentText(component, "Course", "机器学习", "JNU/Label/Small", "color/text-primary", 32);
  title.textAlignHorizontal = "CENTER";
  const room = componentText(component, "Room", "A414", "JNU/Label/Small", "color/text-secondary", 32);
  room.textAlignHorizontal = "CENTER";
  const titleProperty = component.addComponentProperty("Course", "TEXT", "机器学习");
  const roomProperty = component.addComponentProperty("Room", "TEXT", "A414");
  title.componentPropertyReferences = { characters: titleProperty };
  room.componentPropertyReferences = { characters: roomProperty };
  return component;
}

function createChipVariant(tone) {
  const component = figma.createComponent();
  component.name = `Tone=${tone}`;
  component.layoutMode = "HORIZONTAL";
  component.primaryAxisSizingMode = "FIXED";
  component.counterAxisSizingMode = "FIXED";
  component.primaryAxisAlignItems = "CENTER";
  component.counterAxisAlignItems = "CENTER";
  component.resize(92, 30);
  bindNumber(component, "cornerRadius", "radius/full", 999);
  const color = tone === "Success" ? "color/secondary-container" : tone === "Warning" ? "color/surface-variant" : "color/primary-container";
  setFill(component, color, tone === "Success" ? "#005143" : tone === "Warning" ? "#3A2B21" : "#2C438C");
  const label = componentText(component, "Label", tone === "Success" ? "已达标" : tone === "Warning" ? "待处理" : "进行中", "JNU/Label/Small", tone === "Success" ? "color/secondary" : tone === "Warning" ? "color/warning" : "color/on-primary-container");
  const property = component.addComponentProperty("Label", "TEXT", label.characters);
  label.componentPropertyReferences = { characters: property };
  return component;
}

function combine(name, variants, parent) {
  const set = figma.combineAsVariants(variants, parent);
  set.name = name;
  set.layoutMode = "HORIZONTAL";
  set.layoutWrap = "WRAP";
  set.primaryAxisSizingMode = "AUTO";
  set.counterAxisSizingMode = "AUTO";
  set.itemSpacing = 16;
  set.counterAxisSpacing = 16;
  set.paddingTop = 16;
  set.paddingRight = 16;
  set.paddingBottom = 16;
  set.paddingLeft = 16;
  set.fills = [];
  setStroke(set, "color/outline", "#454B59", 0.5);
  bindNumber(set, "cornerRadius", "radius/sm", 12);
  return set;
}

async function buildComponents() {
  requireFoundations();
  const root = rebuildRoot(ROOT_KEYS.components, "JNU • Components", "phase2", 1540, 1780);
  stretch(root, titleBlock("Reusable UI", "Core Components", "Local components match the production Compose and RemoteViews patterns. Re-run downstream phases after rebuilding components.", 1444));

  const specs = [
    ["Button", [createButtonVariant("Primary", "Default"), createButtonVariant("Tonal", "Default"), createButtonVariant("Primary", "Disabled")]],
    ["Navigation Item", [createNavVariant(false), createNavVariant(true)]],
    ["Task Row", [createTaskVariant(false), createTaskVariant(true)]],
    ["Course Block", [createCourseVariant("Indigo"), createCourseVariant("Mint"), createCourseVariant("Purple")]],
    ["Status Chip", [createChipVariant("Info"), createChipVariant("Success"), createChipVariant("Warning")]],
  ];
  const ids = [];
  for (const [name, variants] of specs) {
    stretch(root, makeText(name, "JNU/Headline/Small", "color/text-primary", 1444));
    const set = combine(name, variants, root);
    set.layoutAlign = "STRETCH";
    set.description = `JNU Smart Edu ${name} component set`;
    tag(set, `component/${name.toLowerCase().replace(/ /g, "-")}`, "phase2");
    ids.push(set.id);
  }
  return { title: "Phase 2 · Components", summary: `5 个组件集 · ${specs.reduce((sum, item) => sum + item[1].length, 0)} 个变体`, focus: [root] };
}

function requireFoundations() {
  if (!tokenMap["color/background"] || !textStyles["JNU/Body/Medium"]) throw new Error("请先运行 Phase 1 · Foundations。");
}

function componentSets() {
  const root = findTagged(ROOT_KEYS.components);
  if (!root) throw new Error("请先运行 Phase 2 · Components。");
  return Object.fromEntries(root.findAll(node => node.type === "COMPONENT_SET").map(node => [node.name, node]));
}

function variant(set, fragments) {
  const match = set.children.find(node => node.type === "COMPONENT" && fragments.every(fragment => node.name.includes(fragment)));
  if (!match) throw new Error(`组件 ${set.name} 缺少变体：${fragments.join(", ")}`);
  return match;
}

function setInstanceText(instance, name, value) {
  const node = instance.findOne(child => child.type === "TEXT" && child.name === name);
  if (node) node.characters = value;
}

function appBar(title, subtitle, width = 360) {
  const bar = horizontalFrame("App Bar", 12, 12, width);
  bar.primaryAxisSizingMode = "FIXED";
  bar.counterAxisSizingMode = "FIXED";
  bar.resize(width, 64);
  bar.counterAxisAlignItems = "CENTER";
  setFill(bar, "color/background", "#0B0E14");
  const icon = fixedFrame("Destination Icon", 40, 40);
  setFill(icon, "color/primary-container", "#2C438C");
  bindNumber(icon, "cornerRadius", "radius/sm", 12);
  const glyph = makeText(title.slice(0, 1), "JNU/Title/Small", "color/primary");
  glyph.x = 13;
  glyph.y = 9;
  icon.appendChild(glyph);
  bar.appendChild(icon);
  const copy = verticalFrame("Copy", 250, 0, 0);
  copy.appendChild(makeText(title, "JNU/Title/Large", "color/text-primary", 250));
  copy.appendChild(makeText(subtitle, "JNU/Label/Small", "color/text-secondary", 250));
  bar.appendChild(copy);
  return bar;
}

function statusBar(width = 360) {
  const bar = horizontalFrame("Status Bar", 0, 0, width);
  bar.primaryAxisSizingMode = "FIXED";
  bar.counterAxisSizingMode = "FIXED";
  bar.primaryAxisAlignItems = "SPACE_BETWEEN";
  bar.counterAxisAlignItems = "CENTER";
  bar.paddingLeft = 18;
  bar.paddingRight = 18;
  bar.resize(width, 24);
  setFill(bar, "color/background", "#0B0E14");
  bar.appendChild(makeText("13:00", "JNU/Label/Small", "color/text-primary"));
  bar.appendChild(makeText("5G  ▮▮▮  83%", "JNU/Label/Small", "color/text-secondary"));
  return bar;
}

function bottomNav(selected, sets, width = 360) {
  const nav = horizontalFrame("Bottom Navigation", 4, 10, width);
  nav.primaryAxisSizingMode = "FIXED";
  nav.counterAxisSizingMode = "FIXED";
  nav.primaryAxisAlignItems = "CENTER";
  nav.counterAxisAlignItems = "CENTER";
  nav.resize(width, 82);
  setFill(nav, "color/surface-variant", "#202633");
  const tabs = ["首页", "学分", "课表", "考试", "AI"];
  tabs.forEach(label => {
    const item = variant(sets["Navigation Item"], [`Selected=${label === selected ? "True" : "False"}`]).createInstance();
    setInstanceText(item, "Label", label);
    nav.appendChild(item);
  });
  return nav;
}

function phoneShell(name, title, subtitle, selected, sets) {
  const phone = verticalFrame(name, 360, 0, 0);
  phone.primaryAxisSizingMode = "FIXED";
  phone.counterAxisSizingMode = "FIXED";
  phone.resize(360, 800);
  phone.clipsContent = true;
  setFill(phone, "color/background", "#0B0E14");
  bindNumber(phone, "cornerRadius", "radius/xl", 32);
  setStroke(phone, "color/outline", "#454B59", 0.7);
  stretch(phone, statusBar());
  stretch(phone, appBar(title, subtitle));
  const viewport = fixedFrame("Scrollable Content", 360, 630);
  viewport.clipsContent = true;
  viewport.overflowDirection = "VERTICAL";
  setFill(viewport, "color/background", "#0B0E14");
  phone.appendChild(viewport);
  stretch(phone, bottomNav(selected, sets));
  return { phone, viewport };
}

function scrollContent(viewport, gap = 14) {
  const content = verticalFrame("Content", 320, gap, 0);
  content.x = 20;
  content.y = 10;
  viewport.appendChild(content);
  return content;
}

function sectionHeader(title, meta, width = 320) {
  const row = horizontalFrame("Section Header", 8, 0, width);
  row.primaryAxisSizingMode = "FIXED";
  row.primaryAxisAlignItems = "SPACE_BETWEEN";
  row.counterAxisAlignItems = "CENTER";
  row.appendChild(makeText(title, "JNU/Title/Medium", "color/text-primary"));
  row.appendChild(makeText(meta, "JNU/Label/Small", "color/text-secondary"));
  return row;
}

function heroCard(title, value, caption, width = 320) {
  const hero = verticalFrame("Hero Card", width, 9, 20);
  hero.fills = [{
    type: "GRADIENT_LINEAR",
    gradientTransform: [[0.72, 0.45, -0.08], [-0.45, 0.72, 0.36]],
    gradientStops: [
      { position: 0, color: { ...hex("#151D38"), a: 1 } },
      { position: 0.56, color: { ...hex("#3656B3"), a: 1 } },
      { position: 1, color: { ...hex("#087B69"), a: 1 } },
    ],
  }];
  bindNumber(hero, "cornerRadius", "radius/lg", 24);
  stretch(hero, makeText(title, "JNU/Title/Small", "color/on-primary-container", width - 40));
  const valueRow = horizontalFrame("Value", 8, 0);
  valueRow.counterAxisAlignItems = "MAX";
  valueRow.appendChild(makeText(value, "JNU/Headline/Large", "color/text-primary"));
  valueRow.appendChild(makeText("学分", "JNU/Label/Medium", "color/on-primary-container"));
  hero.appendChild(valueRow);
  stretch(hero, makeText(caption, "JNU/Body/Small", "color/on-primary-container", width - 40));
  const track = fixedFrame("Progress Track", width - 40, 7);
  setFill(track, "color/on-primary-container", "#DDE4FF", 0.22);
  bindNumber(track, "cornerRadius", "radius/full", 999);
  const progress = fixedFrame("Progress", (width - 40) * 0.765, 7);
  setFill(progress, "color/secondary", "#72DEC2");
  bindNumber(progress, "cornerRadius", "radius/full", 999);
  track.appendChild(progress);
  hero.appendChild(track);
  return hero;
}

function infoRow(title, meta, accent = "color/primary", width = 320) {
  const row = horizontalFrame("Info Row", 12, 12, width);
  row.primaryAxisSizingMode = "FIXED";
  row.counterAxisSizingMode = "AUTO";
  row.counterAxisAlignItems = "CENTER";
  setFill(row, "color/surface", "#121721");
  bindNumber(row, "cornerRadius", "radius/md", 18);
  const marker = fixedFrame("Marker", 8, 40);
  setFill(marker, accent, accent === "color/secondary" ? "#72DEC2" : "#B7C7FF");
  bindNumber(marker, "cornerRadius", "radius/full", 999);
  row.appendChild(marker);
  const copyWidth = width - 60;
  const copy = verticalFrame("Copy", copyWidth, 1, 0);
  copy.appendChild(makeText(title, "JNU/Title/Small", "color/text-primary", copyWidth));
  copy.appendChild(makeText(meta, "JNU/Body/Small", "color/text-secondary", copyWidth));
  row.appendChild(copy);
  return row;
}

function buttonInstance(sets, type, label) {
  const instance = variant(sets.Button, [`Type=${type}`, "State=Default"]).createInstance();
  setInstanceText(instance, "Label", label);
  return instance;
}

function taskInstance(sets, done, title, meta) {
  const instance = variant(sets["Task Row"], [`Done=${done ? "True" : "False"}`]).createInstance();
  setInstanceText(instance, "Title", title);
  setInstanceText(instance, "Meta", meta);
  return instance;
}

function buildHome(sets) {
  const { phone, viewport } = phoneShell("Home", "首页", "今日概览", "首页", sets);
  const content = scrollContent(viewport);
  stretch(content, heroCard("学习驾驶舱", "122.5", "已完成 6/8 个培养模块"));
  stretch(content, infoRow("教务数据已同步", "课程、课表与考试安排 · 刚刚更新", "color/secondary"));
  stretch(content, sectionHeader("今日课程", "第 13 周"));
  stretch(content, infoRow("机器学习与神经网络", "08:30–10:10 · 番禺教学大楼 A414"));
  const taskHeader = sectionHeader("待办任务", "2 项待完成");
  taskHeader.appendChild(buttonInstance(sets, "Primary", "添加"));
  stretch(content, taskHeader);
  stretch(content, taskInstance(sets, false, "完成课程作业", "7月3日截止 · 点击编辑"));
  stretch(content, taskInstance(sets, true, "提交实验报告", "已完成"));
  stretch(content, sectionHeader("考试安排", "2 场"));
  stretch(content, infoRow("现代企业管理概论", "7月8日 09:00 · 教学大楼 221"));
  return phone;
}

function buildCredit(sets) {
  const { phone, viewport } = phoneShell("Credit", "学分", "培养方案进度", "学分", sets);
  const content = scrollContent(viewport);
  stretch(content, heroCard("已修学分", "122.5", "毕业要求 160.0 学分"));
  stretch(content, buttonInstance(sets, "Tonal", "预测本学期通过后学分"));
  stretch(content, sectionHeader("学分树", "点开模块查看课程"));
  const groups = [
    ["通识教育课程群", "35.5 / 38.0 学分", "差 2.5"],
    ["专业基础课程群", "42.0 / 42.0 学分", "已达标"],
    ["专业核心课程群", "31.0 / 40.0 学分", "差 9.0"],
    ["实践教学课程群", "14.0 / 20.0 学分", "差 6.0"],
  ];
  groups.forEach(([title, meta, status]) => {
    const group = horizontalFrame("Credit Group", 12, 12, 320);
    group.primaryAxisSizingMode = "FIXED";
    group.counterAxisAlignItems = "CENTER";
    setFill(group, "color/surface", "#121721");
    bindNumber(group, "cornerRadius", "radius/md", 18);
    const marker = fixedFrame("Marker", 8, 40);
    setFill(marker, status === "已达标" ? "color/secondary" : "color/primary", status === "已达标" ? "#72DEC2" : "#B7C7FF");
    bindNumber(marker, "cornerRadius", "radius/full", 999);
    group.appendChild(marker);
    const copy = verticalFrame("Copy", 172, 1, 0);
    copy.appendChild(makeText(title, "JNU/Title/Small", "color/text-primary", 172));
    copy.appendChild(makeText(meta, "JNU/Body/Small", "color/text-secondary", 172));
    group.appendChild(copy);
    const chip = variant(sets["Status Chip"], [`Tone=${status === "已达标" ? "Success" : "Info"}`]).createInstance();
    setInstanceText(chip, "Label", status);
    group.appendChild(chip);
    stretch(content, group);
  });
  return phone;
}

function scheduleHeader() {
  const header = horizontalFrame("Schedule Header", 10, 12, 360);
  header.primaryAxisSizingMode = "FIXED";
  header.counterAxisSizingMode = "FIXED";
  header.resize(360, 72);
  header.counterAxisAlignItems = "CENTER";
  setFill(header, "color/surface", "#121721");
  const menu = fixedFrame("Menu", 42, 42);
  setFill(menu, "color/surface-variant", "#202633");
  bindNumber(menu, "cornerRadius", "radius/sm", 12);
  const menuText = makeText("☰", "JNU/Title/Large", "color/text-primary");
  menuText.x = 10;
  menuText.y = 7;
  menu.appendChild(menuText);
  header.appendChild(menu);
  const copy = verticalFrame("Week", 190, 0, 0);
  copy.appendChild(makeText("第 13 周  ▾", "JNU/Title/Large", "color/text-primary", 190));
  copy.appendChild(makeText("2025-2026 第 2 学期", "JNU/Label/Small", "color/text-secondary", 190));
  header.appendChild(copy);
  const hint = makeText("左右滑动切周", "JNU/Label/Small", "color/primary");
  header.appendChild(hint);
  return header;
}

function scheduleGrid(sets) {
  const grid = fixedFrame("Week Grid", 328, 648);
  setFill(grid, "color/background", "#0B0E14");
  const left = 42;
  const col = (328 - left) / 7;
  const top = 52;
  const row = 54;
  const days = ["一\n1", "二\n2", "三\n3", "四\n4", "五\n5", "六\n6", "日\n7"];
  days.forEach((day, index) => {
    const label = makeText(day, "JNU/Label/Small", index === 2 ? "color/on-primary" : "color/text-secondary", col - 4, "Day");
    label.textAlignHorizontal = "CENTER";
    label.x = left + index * col + 2;
    label.y = 10;
    if (index === 2) {
      const selected = fixedFrame("Selected Day", col - 4, 38);
      setFill(selected, "color/primary", "#B7C7FF");
      bindNumber(selected, "cornerRadius", "radius/sm", 12);
      selected.x = left + index * col + 2;
      selected.y = 7;
      grid.appendChild(selected);
    }
    grid.appendChild(label);
  });
  for (let section = 1; section <= 11; section++) {
    const y = top + (section - 1) * row;
    const label = makeText(`${section}\n${["08:30", "09:25", "10:30", "11:25", "12:20", "14:00", "14:55", "15:50", "16:45", "19:00", "19:55"][section - 1]}`, "JNU/Label/Small", "color/text-secondary", 38, "Section");
    label.textAlignHorizontal = "CENTER";
    label.x = 0;
    label.y = y + 6;
    grid.appendChild(label);
    const line = figma.createLine();
    line.x = left;
    line.y = y;
    line.resize(328 - left, 0);
    setStroke(line, "color/outline", "#454B59", 0.28);
    grid.appendChild(line);
  }
  const courses = [
    [0, 5, 2, "网络工程与组网技术", "A114", "Purple"],
    [2, 5, 2, "机器学习", "108", "Indigo"],
    [4, 0, 2, "机器学习与神经网络", "A414", "Mint"],
    [3, 2, 2, "Windows 编程", "108", "Indigo"],
    [6, 2, 2, "现代企业管理概论", "221", "Purple"],
  ];
  courses.forEach(([day, start, span, title, room, tone]) => {
    const course = variant(sets["Course Block"], [`Tone=${tone}`]).createInstance();
    course.resize(col - 4, row * span - 4);
    setInstanceText(course, "Course", title);
    setInstanceText(course, "Room", room);
    course.x = left + day * col + 2;
    course.y = top + start * row + 2;
    grid.appendChild(course);
  });
  return grid;
}

function buildSchedule(sets) {
  const phone = verticalFrame("Schedule", 360, 0, 0);
  phone.primaryAxisSizingMode = "FIXED";
  phone.counterAxisSizingMode = "FIXED";
  phone.resize(360, 800);
  phone.clipsContent = true;
  setFill(phone, "color/background", "#0B0E14");
  bindNumber(phone, "cornerRadius", "radius/xl", 32);
  setStroke(phone, "color/outline", "#454B59", 0.7);
  stretch(phone, statusBar());
  stretch(phone, scheduleHeader());
  const viewport = fixedFrame("Schedule Scroll", 360, 622);
  viewport.overflowDirection = "VERTICAL";
  viewport.clipsContent = true;
  setFill(viewport, "color/background", "#0B0E14");
  const grid = scheduleGrid(sets);
  grid.x = 16;
  grid.y = 0;
  viewport.appendChild(grid);
  phone.appendChild(viewport);
  stretch(phone, bottomNav("课表", sets));
  return phone;
}

function overlayFrame(name, width = 360, height = 800, opacity = 0.62) {
  const overlay = fixedFrame(name, width, height);
  overlay.fills = [paint("#000000", opacity)];
  return overlay;
}

function buildScheduleDrawer(schedule) {
  const phone = schedule.clone();
  phone.name = "Schedule / Drawer Open";
  const scrim = overlayFrame("Scrim");
  phone.appendChild(scrim);
  scrim.layoutPositioning = "ABSOLUTE";
  scrim.x = 0;
  scrim.y = 0;
  const drawer = verticalFrame("Drawer", 292, 14, 20);
  drawer.primaryAxisSizingMode = "FIXED";
  drawer.counterAxisSizingMode = "FIXED";
  drawer.resize(292, 800);
  setFill(drawer, "color/surface", "#121721");
  stretch(drawer, makeText("课表设置", "JNU/Headline/Small", "color/text-primary", 252));
  stretch(drawer, makeText("管理开学日期与学期课表", "JNU/Body/Small", "color/text-secondary", 252));
  const dateCard = card("Semester Start", 252, 8, 14, "color/surface-variant");
  stretch(dateCard, makeText("开学日期", "JNU/Label/Medium", "color/text-secondary", 224));
  stretch(dateCard, makeText("2026-03-02", "JNU/Title/Large", "color/text-primary", 224));
  stretch(dateCard, makeText("当前第 13 周", "JNU/Body/Small", "color/primary", 224));
  stretch(drawer, dateCard);
  stretch(drawer, makeText("学期课表", "JNU/Title/Medium", "color/text-primary", 252));
  stretch(drawer, infoRow("2025-2026 第 2 学期", "当前使用", "color/primary", 252));
  stretch(drawer, infoRow("2025-2026 第 1 学期", "20 周", "color/text-secondary", 252));
  phone.appendChild(drawer);
  drawer.layoutPositioning = "ABSOLUTE";
  drawer.x = 0;
  drawer.y = 0;
  return phone;
}

function buildExam(sets) {
  const { phone, viewport } = phoneShell("Exam", "考试", "提醒与座位", "考试", sets);
  const content = scrollContent(viewport);
  stretch(content, infoRow("考试提醒已开启", "将在考前 7 天、1 天和 2 小时提醒", "color/secondary"));
  stretch(content, sectionHeader("考试安排", "2 场"));
  stretch(content, infoRow("现代企业管理概论", "7月8日 09:00 · 教学大楼 221 · 座位 18"));
  stretch(content, infoRow("机器学习", "7月12日 14:30 · 知识产权楼 108 · 座位 06"));
  stretch(content, sectionHeader("说明", "本地提醒"));
  const note = card("Reminder Note", 320, 8, 16, "color/surface-variant");
  stretch(note, makeText("考试数据只在本机处理", "JNU/Title/Small", "color/text-primary", 288));
  stretch(note, makeText("重新同步后会自动更新考试时间、地点和座位信息。", "JNU/Body/Small", "color/text-secondary", 288));
  stretch(content, note);
  return phone;
}

function buildAdvisor(sets) {
  const { phone, viewport } = phoneShell("AI Advisor", "AI", "选课建议", "AI", sets);
  const content = scrollContent(viewport);
  const api = card("API Key", 320, 8, 16, "color/surface");
  stretch(api, makeText("DeepSeek API", "JNU/Title/Medium", "color/text-primary", 288));
  stretch(api, makeText("sk-••••••••••••••••••••", "JNU/Body/Medium", "color/text-secondary", 288));
  stretch(api, makeText("密钥仅保存在当前设备", "JNU/Body/Small", "color/secondary", 288));
  stretch(content, api);
  stretch(content, buttonInstance(sets, "Primary", "生成选课建议"));
  const summary = card("Advice Summary", 320, 8, 16, "color/primary-container");
  stretch(summary, makeText("本学期建议", "JNU/Title/Medium", "color/on-primary-container", 288));
  stretch(summary, makeText("优先补齐专业核心课程群，同时控制总学分和考试冲突。", "JNU/Body/Medium", "color/on-primary-container", 288));
  stretch(content, summary);
  stretch(content, sectionHeader("推荐课程", "已计入正在修课程"));
  stretch(content, infoRow("操作系统", "3.0 学分 · 专业核心 · 优先级高", "color/secondary"));
  stretch(content, infoRow("计算机网络", "3.0 学分 · 专业基础 · 无时间冲突"));
  return phone;
}

function buildTaskDialog(home) {
  const phone = home.clone();
  phone.name = "Home / Add Task";
  const scrim = overlayFrame("Scrim", 360, 800, 0.68);
  phone.appendChild(scrim);
  scrim.layoutPositioning = "ABSOLUTE";
  scrim.x = 0;
  scrim.y = 0;
  const dialog = verticalFrame("Add Task Dialog", 320, 14, 20);
  setFill(dialog, "color/surface", "#121721");
  bindNumber(dialog, "cornerRadius", "radius/lg", 24);
  setStroke(dialog, "color/outline", "#454B59", 0.8);
  stretch(dialog, makeText("添加待办任务", "JNU/Title/Large", "color/text-primary", 280));
  const field = card("Task Field", 280, 4, 12, "color/surface-variant");
  stretch(field, makeText("任务标题", "JNU/Label/Small", "color/text-secondary", 256));
  stretch(field, makeText("完成课程作业", "JNU/Body/Medium", "color/text-primary", 256));
  stretch(dialog, field);
  const date = card("Date Field", 280, 4, 12, "color/surface-variant");
  stretch(date, makeText("截止日期", "JNU/Label/Small", "color/text-secondary", 256));
  stretch(date, makeText("2026-07-03", "JNU/Body/Medium", "color/text-primary", 256));
  stretch(dialog, date);
  const actions = horizontalFrame("Actions", 10, 0);
  actions.appendChild(makeText("取消", "JNU/Label/Medium", "color/text-secondary"));
  const add = fixedFrame("Add", 84, 40);
  setFill(add, "color/primary", "#B7C7FF");
  bindNumber(add, "cornerRadius", "radius/sm", 12);
  const addText = makeText("添加", "JNU/Label/Medium", "color/on-primary");
  addText.x = 28;
  addText.y = 11;
  add.appendChild(addText);
  actions.appendChild(add);
  dialog.appendChild(actions);
  phone.appendChild(dialog);
  dialog.layoutPositioning = "ABSOLUTE";
  dialog.x = 20;
  dialog.y = 190;
  return phone;
}

async function buildScreens() {
  requireFoundations();
  const sets = componentSets();
  const root = rebuildRoot(ROOT_KEYS.screens, "JNU • App Screens", "phase3", 1740, 3520);
  stretch(root, titleBlock("Android · 360 × 800", "App Screens & States", "Seven production-facing states using compact navigation, scroll-first content, readable schedule blocks, and explicit task workflows.", 1644));
  const rowOne = horizontalFrame("Primary Screens", 24, 0);
  const home = buildHome(sets);
  const credit = buildCredit(sets);
  const schedule = buildSchedule(sets);
  rowOne.appendChild(home);
  rowOne.appendChild(credit);
  rowOne.appendChild(schedule);
  rowOne.appendChild(buildScheduleDrawer(schedule));
  stretch(root, rowOne);
  const rowTwo = horizontalFrame("Secondary Screens", 24, 0);
  rowTwo.appendChild(buildExam(sets));
  rowTwo.appendChild(buildAdvisor(sets));
  rowTwo.appendChild(buildTaskDialog(home));
  stretch(root, rowTwo);
  return { title: "Phase 3 · App Screens", summary: "7 个完整状态：首页、学分、课表、课表侧栏、考试、AI、添加待办", focus: [root] };
}

function widgetShell(name, width, height) {
  const widget = verticalFrame(name, width, 10, 16);
  widget.primaryAxisSizingMode = "FIXED";
  widget.counterAxisSizingMode = "FIXED";
  widget.resize(width, height);
  widget.clipsContent = true;
  setFill(widget, "color/surface", "#121721");
  bindNumber(widget, "cornerRadius", "radius/lg", 24);
  setStroke(widget, "color/outline", "#454B59", 0.65);
  return widget;
}

function widgetHeader(title, count, width) {
  const header = horizontalFrame("Open App Header", 10, 0, width);
  header.primaryAxisSizingMode = "FIXED";
  header.primaryAxisAlignItems = "SPACE_BETWEEN";
  header.counterAxisAlignItems = "CENTER";
  const left = horizontalFrame("Title", 10, 0);
  const icon = fixedFrame("Icon", 32, 32);
  setFill(icon, "color/secondary", "#72DEC2");
  bindNumber(icon, "cornerRadius", "radius/sm", 12);
  const glyph = makeText("✓", "JNU/Title/Small", "color/on-primary");
  glyph.x = 10;
  glyph.y = 6;
  icon.appendChild(glyph);
  left.appendChild(icon);
  left.appendChild(makeText(title, "JNU/Title/Medium", "color/text-primary"));
  header.appendChild(left);
  const badge = fixedFrame("Count", 30, 26);
  setFill(badge, "color/primary-container", "#2C438C");
  bindNumber(badge, "cornerRadius", "radius/full", 999);
  const countText = makeText(String(count), "JNU/Label/Small", "color/primary");
  countText.x = 11;
  countText.y = 5;
  badge.appendChild(countText);
  header.appendChild(badge);
  return header;
}

function buildTaskEmptyWidget() {
  const widget = widgetShell("Task Widget / Empty", 320, 190);
  stretch(widget, widgetHeader("待办任务", 0, 288));
  const empty = verticalFrame("Empty State", 288, 7, 0);
  empty.primaryAxisAlignItems = "CENTER";
  empty.counterAxisAlignItems = "CENTER";
  const add = fixedFrame("Add Task", 52, 52);
  setFill(add, "color/secondary", "#72DEC2");
  bindNumber(add, "cornerRadius", "radius/md", 18);
  const plus = makeText("+", "JNU/Headline/Small", "color/on-primary");
  plus.x = 18;
  plus.y = 9;
  add.appendChild(plus);
  empty.appendChild(add);
  empty.appendChild(makeText("添加第一条待办", "JNU/Body/Small", "color/text-secondary"));
  stretch(widget, empty);
  return widget;
}

function buildTaskListWidget() {
  const widget = widgetShell("Task Widget / List", 320, 238);
  stretch(widget, widgetHeader("待办任务", 3, 288));
  const tasks = [["完成课程作业", "7月3日截止"], ["提交实验报告", "7月5日截止"], ["复习机器学习", "7月7日截止"]];
  tasks.forEach(([title, date]) => {
    const row = horizontalFrame("Complete Task", 10, 9, 288);
    row.primaryAxisSizingMode = "FIXED";
    row.counterAxisAlignItems = "CENTER";
    setFill(row, "color/surface-variant", "#202633", 0.72);
    bindNumber(row, "cornerRadius", "radius/sm", 12);
    const check = fixedFrame("Check", 22, 22);
    check.fills = [];
    setStroke(check, "color/secondary", "#72DEC2");
    bindNumber(check, "cornerRadius", "radius/full", 999);
    row.appendChild(check);
    const copy = verticalFrame("Copy", 230, 0, 0);
    copy.appendChild(makeText(title, "JNU/Title/Small", "color/text-primary", 230));
    copy.appendChild(makeText(date, "JNU/Label/Small", "color/text-secondary", 230));
    row.appendChild(copy);
    stretch(widget, row);
  });
  return widget;
}

function buildScheduleWidget() {
  const widget = widgetShell("Schedule Widget", 360, 260);
  const header = horizontalFrame("Open Schedule Header", 8, 0, 328);
  header.primaryAxisSizingMode = "FIXED";
  header.primaryAxisAlignItems = "SPACE_BETWEEN";
  const copy = verticalFrame("Date", 240, 0, 0);
  copy.appendChild(makeText("今日课程", "JNU/Title/Medium", "color/text-primary", 240));
  copy.appendChild(makeText("7月1日 星期三", "JNU/Label/Small", "color/text-secondary", 240));
  header.appendChild(copy);
  const icon = fixedFrame("Course", 34, 34);
  setFill(icon, "color/secondary", "#72DEC2");
  bindNumber(icon, "cornerRadius", "radius/sm", 12);
  const iconText = makeText("课", "JNU/Label/Medium", "color/on-primary");
  iconText.x = 10;
  iconText.y = 7;
  icon.appendChild(iconText);
  header.appendChild(icon);
  stretch(widget, header);
  const lessons = [["08:30", "机器学习与神经网络", "A414"], ["10:30", "Windows 编程", "108"], ["14:00", "计算机网络", "A114"]];
  lessons.forEach(([time, title, room]) => {
    const row = horizontalFrame("Course Row", 10, 9, 328);
    row.primaryAxisSizingMode = "FIXED";
    row.counterAxisAlignItems = "CENTER";
    setFill(row, "color/surface-variant", "#202633", 0.72);
    bindNumber(row, "cornerRadius", "radius/sm", 12);
    row.appendChild(makeText(time, "JNU/Label/Small", "color/primary"));
    const titleText = makeText(title, "JNU/Title/Small", "color/text-primary", 200);
    row.appendChild(titleText);
    row.appendChild(makeText(room, "JNU/Label/Small", "color/text-secondary"));
    stretch(widget, row);
  });
  const exam = horizontalFrame("Open Exam", 8, 10, 328);
  exam.primaryAxisSizingMode = "FIXED";
  setFill(exam, "color/surface-variant", "#202633");
  bindNumber(exam, "cornerRadius", "radius/sm", 12);
  exam.appendChild(makeText("考试", "JNU/Label/Small", "color/warning"));
  exam.appendChild(makeText("现代企业管理概论 · 7月8日 09:00", "JNU/Label/Small", "color/text-primary"));
  stretch(widget, exam);
  return widget;
}

function qaCard(title, items, width = 440) {
  const frame = card(title, width, 10, 18, "color/surface-variant");
  stretch(frame, makeText(title, "JNU/Title/Large", "color/text-primary", width - 36));
  items.forEach(item => stretch(frame, makeText(`✓  ${item}`, "JNU/Body/Small", "color/text-secondary", width - 36)));
  return frame;
}

async function buildWidgetsAndQa() {
  requireFoundations();
  componentSets();
  const root = rebuildRoot(ROOT_KEYS.widgets, "JNU • Widgets & QA", "phase4", 1540, 5280);
  stretch(root, titleBlock("Launcher & Handoff", "Widgets, Interactions and QA", "Responsive launcher surfaces, explicit tap targets, empty states, and implementation acceptance checks.", 1444));
  const widgets = horizontalFrame("Widget States", 24, 0);
  widgets.counterAxisAlignItems = "MIN";
  widgets.appendChild(buildTaskEmptyWidget());
  widgets.appendChild(buildTaskListWidget());
  widgets.appendChild(buildScheduleWidget());
  stretch(root, widgets);
  const qa = horizontalFrame("QA", 24, 0);
  qa.appendChild(qaCard("Interaction contract", [
    "待办圆形勾选区直接完成任务并刷新小部件",
    "待办标题和顶栏打开应用首页",
    "空待办状态的加号直接打开添加弹窗",
    "今日课程顶栏进入课表，考试卡进入考试页",
  ]));
  qa.appendChild(qaCard("Accessibility", [
    "主要触控区域至少 44 × 44dp",
    "正文与背景保持可读对比度",
    "颜色不作为完成、告警和选中状态的唯一提示",
    "长课程名保留多行空间，教室信息始终可见",
  ]));
  qa.appendChild(qaCard("Responsive checks", [
    "手机课表一屏显示七天，不提供水平滚动",
    "课程纵向滚动，双节课程高度紧凑",
    "小部件可缩放时优先保留标题与第一条内容",
    "空、部分导入和同步失败状态都有用户文案",
  ]));
  stretch(root, qa);
  return { title: "Phase 4 · Widgets & QA", summary: "3 个小部件状态 · 12 条交互、无障碍和响应式验收规则", focus: [root] };
}
