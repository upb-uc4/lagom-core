kubectl exec --stdin svc/postgres -- psql -U postgresadmin
\l
sleep 60