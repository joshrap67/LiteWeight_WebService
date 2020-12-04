if not exist .\site-packages\ pip install -r requirements.txt -t .\site-packages
xcopy /y .\auto_confirm.py .\site-packages
cd site-packages
7z.exe a lambda_package.zip .
MOVE /Y .\lambda_package.zip ..