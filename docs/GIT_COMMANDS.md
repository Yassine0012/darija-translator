# Publishing to GitHub (several commits so the history is meaningful)

Create an empty repository on GitHub first (no README), then from the project root:

```cmd
git init
git branch -M main

git add server
git commit -m "Add REST service: Jersey, embedded Tomcat, Gemini translation, Jakarta Authentication"

git add chrome-extension
git commit -m "Add Chrome extension (Manifest V3, side panel, context menu)"

git add clients
git commit -m "Add Python and PHP clients"

git add mobile
git commit -m "Add React Native mobile client"

git add docs
git commit -m "Add UML diagrams and demo script"

git add README.md .gitignore
git commit -m "Add README"

git remote add origin https://github.com/YOUR_USER/YOUR_REPO.git
git push -u origin main
```

Before pushing, check that no secret is committed: `git grep -n "AIza"` should print nothing.
