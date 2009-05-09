#! /bin/bash

getPassword () {
	stty -echo
	echo "Enter the password for $username:"
	read password

	echo "Enter the password again:"
	read passwordConfirm
	stty echo
}


# 1 -- Determine if a JRE exists
echo
echo
echo "Step 1 of 4: Make sure a Java runtime exists"
echo "---------------------------------------------------"
javaCheck=$(java)
if [ "${#javaCheck}" = "0" ]
then
	echo "No Java runtime was found. Please install a Java runtime (JRE) to run NSIA."
	exit 1
else
	echo "Java runtime was successfully found"
fi


# 2 -- Determine if the system appears to have been configured already
echo
echo
echo "Step 2 of 4: Make sure that NSIA has not been configured yet"
echo "---------------------------------------------------"
if [ -d Database ]
then
	echo "NSIA appears to have been installed already. This install script can only be run when NSIA has not been started yet."
	exit 1
else
	echo "NSIA does not appear to have been configured yet"
fi


# 3 -- Get the username
echo
echo
echo "Step 3 of 4: Setup the administrator account"
echo "---------------------------------------------------"
echo "Enter a username for the administrator account. Note that the username can contain letters, numbers, dashes and periods:"


while :
do

	read username
	
	if [ "${#username}" = "0" ]
	then
		isInvalid=1
	else
		isInvalid=$(echo $username | sed 's/[a-zA-Z0-9_.-]*//')
	fi
	
	if [ "${#isInvalid}" != "0" ]
	then
		echo "The username is invalid. The username must only contain letters, numbers, dashes and periods."
	elif [ "${#username}" = "0" ]
	then
		echo "The username is invalid. The username must only contain letters, numbers, dashes and periods."
	else
		break
	fi
	
	echo "Please try again:"
	
	
done


# 4 -- Get the password
while :
do
	getPassword
	if [ "$password" = "$passwordConfirm" ]
	then
	
		if [ "${#password}" -gt 7 ]
		then
			break
		else
			echo "The passwords must be at least 8 characters, please try again"
		fi
		
	else
		echo "The passwords did not match, please try again"
	fi
done


# 5 -- Initialize the application
echo
echo
echo "Step 4 of 4: Initialize NSIA"
echo "---------------------------------------------------"
java -jar nsia.jar --install $username $username $password

# 6 -- Print out the success message and give the user the option to run NSIA now
echo
echo "NSIA was successfully installed!"
echo "Now, all you have to do is run it"

#	 6.1 -- Tell the user how to run it in the future
echo
echo "If you are on an Intel platform, you can start NSIA by running the \"ThreatFactor NSIA\" binary in the installation directory"
echo
echo "Otherwise, you can run it with the following command: \"java -jar nsia.jar\""

#	 6.2 -- Give the option to run it now
echo "Would you like to run NSIA now?"
echo "[y]es or [n]o?"

read runnow

if [ "${runnow}" == "y" ]
then
	echo
	echo
	java -jar nsia.jar
elif [ "${runnow}" == "yes" ]
then
	echo
	echo
	java -jar nsia.jar
else
	echo "Alright, goodbye"
fi

