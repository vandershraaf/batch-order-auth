### Batch Order Auth Tool

This tool is written mainly to run authorizations on order in batch (although
you will have option to do so for single authorization). That is, if you have 
order authorization (the id look something like O-XXXXXXXXXXXXXXXX), run authorization
on the order so that you can capture it. This tool uses DoAuthorization API
(https://developer.paypal.com/docs/classic/api/merchant/DoAuthorization_API_Operation_NVP/).

For more information the order authorization in PayPal, refer here: https://developer.paypal.com/docs/classic/admin/auth-capture/

##### Section 1:
##### Before running this tool (Pre-requisite)

1) Make sure you have Java installed in your machine. Refer here for more information: 
https://java.com/en/download/help/download_options.xml
2) Make sure you have included Java in your machine's PATH. If you cannot add it to 
PATH, reference the directory that includes Java executable when running the tool. 
You can find more information here: 
https://www.java.com/en/download/help/path.xml
3) If you want to run authorizations in batch from a file, prepare a comma-delimited file that
contains orders and amount that you need to authorize. That is, each row should have
the following format:
[Order Id],[Amount to authorize]

Refer to "example.csv" file on the required format. 


##### Section 2:
##### During running this tool (How to run this tool)

1) Open up command prompt or terminal in your machine
2) Change directory to the directory that contains the JAR file. For example, in
Windows, use the following command:
cd C:\YOUR_FOLDER_PATH
3) Run the following command:
java -jar BatchAuth-1.0-jar-with-dependencies.jar
4) For the first few prompts, you will be required to enter API credentials of your account, 
which are API username, API password, and API signature. Refer here for more information
on how you can get the API credentials from your account:
https://www.paypal-knowledge.com/infocenter/index?page=content&id=FAQ1454&expand=true&locale=en_US
5) Enter the mode of environment. That is, if you want to run API in sandbox, type in "sandbox" or "Sandbox"
6) Select application mode. Enter "1" or "2" or "3" once prompted

###### For "Single order authorization" mode
7) Enter the order id. It should start with 'O-'
8) Enter the amount that you want to authorize. Make sure it is in integer or double format
9) Enter currency 
10) If authorization is successful, you will see the [SUCCESS] tag. Otherwise, you will see [FAILURE] tag
	
###### For "Multiple orders authorization (from file)" mode
7) Enter the file name that contains input orders. Refer to Section 1, Step 3 for more info on the file format
8) Enter currency
9) This tool will read the file and try submitting authorizations one by one. It should show whether an authorization
	has failed together with error codes and error message
10) Once completed, it should prompt how many authorizations failed and successful. Also, it should output 'batch_file_*.log' file
	containing list of successful or failed authorizations

###### For "Multiple orders authorization (from API)" mode
7) Enter start date
8) Enter end date
9) This mode will search your account for any pending orders and run authorizations on the orders
10) Once completed, it should prompt how many authorizations failed and successful. Also, it should output 'batch_file_*.log' file
	containing list of successful or failed authorizations
		


