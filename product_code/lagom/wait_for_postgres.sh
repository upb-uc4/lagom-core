until PGPASSWORD="admin" psql -U "admin" -c '\q'; do
  sleep 10
done