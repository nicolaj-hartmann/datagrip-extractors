# datagrip-extractors

Repository of custom DataGrip extractors designed to simplify data extraction, including a multi-row insert extractor for MSSQL. These tools help automate and customize data operations in DataGrip for more efficient workflows.

```bash install single script
DatagripVersion=2023.3 && \
curl -s -o "/Users/$(whoami)/Library/Application Support/JetBrains/DataGrip${DatagripVersion}/extensions/com.intellij.database/data/extractors/mssql-multirow-insert.groovy" \
"https://raw.githubusercontent.com/nicolaj-hartmann/datagrip-extractors/refs/heads/main/scripts/mssql-multirow-insert.groovy"
```
