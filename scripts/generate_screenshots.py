"""
Script to capture authentic screenshots of the running Hospital Management System
and render real source code in a VS Code style editor window for a LinkedIn showcase.
"""

import os
import sys
import time
from PIL import Image, ImageDraw, ImageFont
import pygments
from pygments.lexers import JavaLexer
from pygments.formatters import HtmlFormatter
from selenium import webdriver
from selenium.webdriver.edge.options import Options
from selenium.webdriver.common.by import By

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "screenshots")
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ----------------------------------------------------------------------
# 1. Capture Running Web Application Screens
# ----------------------------------------------------------------------
def capture_web_screens():
    print("[1/3] Capturing real application screens from running Spring Boot server...")
    edge_options = Options()
    edge_options.add_argument("--headless=new")
    edge_options.add_argument("--window-size=1440,960")
    edge_options.add_argument("--disable-gpu")
    edge_options.add_argument("--hide-scrollbars")

    driver = webdriver.Edge(options=edge_options)
    try:
        driver.set_window_size(1440, 960)

        # Login as admin
        driver.get("http://localhost:8080/login")
        time.sleep(1)
        driver.find_element(By.NAME, "username").send_keys("admin")
        driver.find_element(By.NAME, "password").send_keys("AdminPass123!")
        driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
        time.sleep(2)

        # Dashboard
        driver.get("http://localhost:8080/")
        time.sleep(1.5)
        dash_path = os.path.join(OUTPUT_DIR, "dashboard.png")
        driver.save_screenshot(dash_path)
        print("  -> Saved dashboard.png")

        # Patient Management
        driver.get("http://localhost:8080/patients")
        time.sleep(1.5)
        patient_path = os.path.join(OUTPUT_DIR, "patient-management.png")
        driver.save_screenshot(patient_path)
        print("  -> Saved patient-management.png")

        # Appointments
        driver.get("http://localhost:8080/appointments")
        time.sleep(1.5)
        appts_path = os.path.join(OUTPUT_DIR, "appointments.png")
        driver.save_screenshot(appts_path)
        print("  -> Saved appointments.png")

    finally:
        driver.quit()


# ----------------------------------------------------------------------
# 2. Render Authentic Code Screenshots in VS Code Window Style
# ----------------------------------------------------------------------
def render_code_to_html(source_code, file_path_display, file_name, start_line_num=1):
    lexer = JavaLexer()
    formatter = HtmlFormatter(style="monokai", nowrap=True)
    highlighted_code = pygments.highlight(source_code, lexer, formatter)

    lines = highlighted_code.strip().split("\n")
    numbered_lines = []
    for i, line in enumerate(lines, start=start_line_num):
        numbered_lines.append(
            f'<div class="code-line"><span class="line-num">{i}</span><span class="line-content">{line}</span></div>'
        )
    body_content = "\n".join(numbered_lines)

    css = formatter.get_style_defs('.highlight')

    html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
    * {{
        box-sizing: border-box;
        margin: 0;
        padding: 0;
    }}
    body {{
        background-color: #181a1f;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        padding: 30px;
        display: flex;
        justify-content: center;
        align-items: center;
    }}
    .window {{
        width: 1200px;
        background-color: #1e1e1e;
        border-radius: 10px;
        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.65), 0 0 0 1px rgba(255, 255, 255, 0.08);
        overflow: hidden;
        border: 1px solid #333842;
    }}
    .titlebar {{
        background: #252526;
        height: 38px;
        display: flex;
        align-items: center;
        padding: 0 16px;
        border-bottom: 1px solid #2d2d2d;
        position: relative;
    }}
    .controls {{
        display: flex;
        gap: 8px;
    }}
    .dot {{
        width: 12px;
        height: 12px;
        border-radius: 50%;
    }}
    .dot.red {{ background: #ff5f56; }}
    .dot.yellow {{ background: #ffbd2e; }}
    .dot.green {{ background: #27c93f; }}
    .title {{
        position: absolute;
        left: 0;
        right: 0;
        text-align: center;
        font-size: 13px;
        color: #999999;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        letter-spacing: 0.3px;
    }}
    .tabbar {{
        background: #2d2d2d;
        display: flex;
        align-items: center;
        height: 35px;
        border-bottom: 1px solid #1e1e1e;
    }}
    .tab {{
        background: #1e1e1e;
        color: #cccccc;
        font-size: 13px;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        padding: 0 18px;
        height: 100%;
        display: flex;
        align-items: center;
        gap: 8px;
        border-top: 2px solid #007acc;
    }}
    .tab .icon {{
        color: #e06c75;
        font-weight: bold;
        font-size: 11px;
    }}
    .breadcrumbs {{
        background: #1e1e1e;
        padding: 8px 20px;
        font-size: 12px;
        color: #7a7a7a;
        border-bottom: 1px solid #282828;
    }}
    .editor {{
        background-color: #1e1e1e;
        padding: 16px 0 20px 0;
        font-family: 'Consolas', 'Courier New', 'Fira Code', monospace;
        font-size: 14.5px;
        line-height: 1.55;
    }}
    .code-line {{
        display: flex;
        padding: 0 20px;
    }}
    .code-line:hover {{
        background-color: #262626;
    }}
    .line-num {{
        width: 45px;
        color: #5c6370;
        text-align: right;
        padding-right: 22px;
        user-select: none;
        flex-shrink: 0;
    }}
    .line-content {{
        white-space: pre;
        color: #abb2bf;
    }}
    .statusbar {{
        background: #007acc;
        color: #ffffff;
        font-size: 12px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 14px;
    }}
    .statusbar span {{
        margin-right: 14px;
    }}
    {css}
</style>
</head>
<body>
    <div class="window">
        <div class="titlebar">
            <div class="controls">
                <div class="dot red"></div>
                <div class="dot yellow"></div>
                <div class="dot green"></div>
            </div>
            <div class="title">{file_name} — Hospital Management System</div>
        </div>
        <div class="tabbar">
            <div class="tab">
                <span class="icon">☕</span>
                <span>{file_name}</span>
            </div>
        </div>
        <div class="breadcrumbs">hospital_management &gt; {file_path_display}</div>
        <div class="editor">
            {body_content}
        </div>
        <div class="statusbar">
            <div>
                <span>main*</span>
                <span>0 errors, 0 warnings</span>
            </div>
            <div>
                <span>Ln {start_line_num}, Col 1</span>
                <span>Spaces: 4</span>
                <span>UTF-8</span>
                <span>Java 21</span>
                <span>Spring Boot 4</span>
            </div>
        </div>
    </div>
</body>
</html>"""
    return html


def capture_code_screens():
    print("[2/3] Generating authentic code screenshots in VS Code editor theme...")
    edge_options = Options()
    edge_options.add_argument("--headless=new")
    edge_options.add_argument("--window-size=1360,980")
    edge_options.add_argument("--disable-gpu")
    edge_options.add_argument("--hide-scrollbars")

    driver = webdriver.Edge(options=edge_options)
    driver.set_window_size(1360, 980)

    code_examples = [
        {
            "id": "04_backend_controller",
            "file": os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "hospital", "hospital_management", "controller", "AppointmentController.java"),
            "display_path": "src > main > java > com > hospital > controller > AppointmentController.java",
            "filename": "AppointmentController.java",
            "start": 52,
            "end": 96
        },
        {
            "id": "05_service_logic",
            "file": os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "hospital", "hospital_management", "service", "AppointmentService.java"),
            "display_path": "src > main > java > com > hospital > service > AppointmentService.java",
            "filename": "AppointmentService.java",
            "start": 66,
            "end": 115
        },
        {
            "id": "06_database_entity",
            "file": os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "hospital", "hospital_management", "model", "Appointment.java"),
            "display_path": "src > main > java > com > hospital > model > Appointment.java",
            "filename": "Appointment.java",
            "start": 19,
            "end": 64
        },
        {
            "id": "07_security_config",
            "file": os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "hospital", "hospital_management", "config", "SecurityConfig.java"),
            "display_path": "src > main > java > com > hospital > config > SecurityConfig.java",
            "filename": "SecurityConfig.java",
            "start": 27,
            "end": 74
        }
    ]

    temp_html_path = os.path.join(OUTPUT_DIR, "_temp_code.html")

    for ex in code_examples:
        with open(ex["file"], "r", encoding="utf-8") as f:
            all_lines = f.readlines()

        start_idx = ex["start"] - 1
        end_idx = ex["end"]
        snippet = "".join(all_lines[start_idx:end_idx])

        html_content = render_code_to_html(
            snippet,
            ex["display_path"],
            ex["filename"],
            start_line_num=ex["start"]
        )

        with open(temp_html_path, "w", encoding="utf-8") as f:
            f.write(html_content)

        driver.get("file:///" + temp_html_path.replace("\\", "/"))
        time.sleep(1)

        win_elem = driver.find_element(By.CLASS_NAME, "window")
        out_img_path = os.path.join(OUTPUT_DIR, f"{ex['id']}.png")
        win_elem.screenshot(out_img_path)
        print(f"  -> Saved {ex['id']}.png")

    if os.path.exists(temp_html_path):
        os.remove(temp_html_path)

    driver.quit()


# ----------------------------------------------------------------------
# 3. Create Collage: 08_project_showcase.png
# ----------------------------------------------------------------------
def create_collage():
    print("[3/3] Creating natural student project showcase collage...")

    img_dash = Image.open(os.path.join(OUTPUT_DIR, "dashboard.png"))
    img_appts = Image.open(os.path.join(OUTPUT_DIR, "appointments.png"))
    img_sec = Image.open(os.path.join(OUTPUT_DIR, "07_security_config.png"))
    img_svc = Image.open(os.path.join(OUTPUT_DIR, "05_service_logic.png"))

    card_w = 900
    card_h = 560

    def fit_card(img, target_w, target_h):
        img_ratio = img.width / img.height
        target_ratio = target_w / target_h

        if img_ratio > target_ratio:
            new_height = target_h
            new_width = int(target_h * img_ratio)
            resized = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
            left = (new_width - target_w) // 2
            return resized.crop((left, 0, left + target_w, target_h))
        else:
            new_width = target_w
            new_height = int(target_w / img_ratio)
            resized = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
            top = 0
            return resized.crop((0, top, target_w, top + target_h))

    c1 = fit_card(img_dash, card_w, card_h)
    c2 = fit_card(img_appts, card_w, card_h)
    c3 = fit_card(img_sec, card_w, card_h)
    c4 = fit_card(img_svc, card_w, card_h)

    padding = 40
    gap = 30
    header_h = 120
    canvas_w = padding * 2 + card_w * 2 + gap
    canvas_h = padding * 2 + header_h + card_h * 2 + gap

    canvas = Image.new("RGB", (canvas_w, canvas_h), color=(244, 246, 249))
    draw = ImageDraw.Draw(canvas)

    try:
        title_font = ImageFont.truetype("arialbd.ttf", 36)
        sub_font = ImageFont.truetype("arial.ttf", 20)
        badge_font = ImageFont.truetype("arialbd.ttf", 16)
    except:
        title_font = ImageFont.load_default()
        sub_font = ImageFont.load_default()
        badge_font = ImageFont.load_default()

    draw.text((padding, padding), "Hospital Management System (HMS)", fill=(18, 30, 49), font=title_font)
    draw.text((padding, padding + 48), "Full-Stack Project Showcase  \u2022  Java 21  |  Spring Boot 4  |  Spring Security  |  MySQL", fill=(85, 95, 110), font=sub_font)

    def paste_card(image, x, y, label_text):
        border_color = (205, 215, 225)
        draw.rectangle([x - 2, y - 2, x + card_w + 1, y + card_h + 1], outline=border_color, width=2)
        canvas.paste(image, (x, y))

        badge_h = 36
        badge_y = y + card_h - badge_h
        draw.rectangle([x, badge_y, x + card_w, y + card_h], fill=(20, 25, 35, 230))
        draw.text((x + 16, badge_y + 8), label_text, fill=(255, 255, 255), font=badge_font)

    x1 = padding
    x2 = padding + card_w + gap
    y1 = padding + header_h
    y2 = y1 + card_h + gap

    paste_card(c1, x1, y1, "01  \u2022  Executive Dashboard & Live Capacity Metrics")
    paste_card(c2, x2, y1, "02  \u2022  Appointment Scheduling & Status Management")
    paste_card(c3, x1, y2, "03  \u2022  Spring Security 6  \u2022  Role-Based Access Control (RBAC)")
    paste_card(c4, x2, y2, "04  \u2022  Business Logic Service  \u2022  Schedule Conflict Prevention")

    collage_path = os.path.join(OUTPUT_DIR, "project-showcase.png")
    canvas.save(collage_path, "PNG", quality=95)
    print(f"  -> Saved {collage_path}")


if __name__ == "__main__":
    try:
        capture_web_screens()
        capture_code_screens()
        create_collage()
        print("\nAll 8 showcase screenshots created successfully!")
    except Exception as e:
        print("Error during execution:", e)
        sys.exit(1)
