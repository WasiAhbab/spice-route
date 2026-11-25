"""Seed an isolated demo workspace through the real API. Standard library only."""
import json, urllib.request, uuid, os
base=os.environ.get('API_URL','http://localhost:8080/api')
def call(path,body=None,token=None):
    headers={'Content-Type':'application/json', 'X-Session-Mode':'bearer'}
    if token: headers['Authorization']='Bearer '+token
    with urllib.request.urlopen(urllib.request.Request(base+path,headers=headers,data=None if body is None else json.dumps(body).encode())) as response: return json.load(response)
email=f'demo-{uuid.uuid4().hex[:8]}@example.test'; password='Demo-'+uuid.uuid4().hex
session=call('/auth/register',{'name':'Demo owner','email':email,'password':password}); token=session['token']
r=call('/manage/restaurants',{'name':'Spice Route'},token)['id']
comments=[(5,'The biryani was excellent and service was quick.','en'),(2,'খাবার ভালো ছিল কিন্তু পরিবেশন করতে অনেক দেরি হয়েছে।','bn'),(3,'Food bhalo chilo but wait onek beshi.','mixed'),(5,'Friendly staff and a clean dining area.','en'),(4,'পরিবেশ সুন্দর, খাবারও ভালো।','bn'),(2,'AC kaj korchilo na, onek gorom chilo.','mixed'),(4,'','en')]
for name in ['Dhanmondi','Gulshan','Uttara','Bashundhara']:
    b=call('/manage/branches',{'restaurantId':r,'name':name},token)['id']
    for i in range(1,5): call('/manage/tables',{'branchId':b,'name':f'T-{i:02}'},token)
    tables=call('/manage/tables/'+b,token=token)
    for i,(rating,comment,language) in enumerate(comments): call('/public/feedback/'+tables[i%len(tables)]['token'],{'rating':rating,'comment':comment,'language':language})
print('Demo login:',email,password)
